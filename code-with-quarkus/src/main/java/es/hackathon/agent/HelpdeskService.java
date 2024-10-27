package es.hackathon.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.hackathon.agent.ia.ResolviaAgent;
import es.hackathon.factory.HelpdeskResponseFactory;
import es.hackathon.factory.TicketInfoFactory;
import es.hackathon.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class HelpdeskService {

    private static final String EN_CURSO = "891";
    private static final String RESOLVE = "761";
    private static final String ESCALATED = "921";
    private static final String ID_ACCOUNT_RESOLVIA = "712020:a597a544-8ead-450a-aa72-6eb6cd826cf5";
    private static final Logger logger = Logger.getLogger(HelpdeskService.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    @RestClient
    HelpdeskRestClient helpDeskRestClient;
    @Inject
    @RestClient
    KnowledgeRestClient knowledgeRestClient;
    @Inject
    @RestClient
    AgentPhoneRestClient agentPhoneRestClient;
    @Inject
    HelpdeskResponseFactory messagesFactory;
    @Inject
    TicketInfoFactory ticketInfoFactory;
    @Inject
    ResolviaAgent resolviaAgent;

    public void newTicketCreated(String key, JsonNode body) {
        try {
        TicketInfoDTO ticketInfoDTO = ticketInfoFactory.createCommentFromNewTicket(body);
        triageTicketResponse(key, ticketInfoDTO);
        }catch (JsonProcessingException e){
            logger.severe("Error al procesar el nuevo ticket");
            throw new RuntimeException(e);
        }
    }

    public void newCommentInTicket(String key, JsonNode body) {

        try {
            if (!isAssigneeResolvia(body) || isAuthorAgent(body)) {
                logger.info("Ignoring comment from atlassian");
                return;
            }

            //Nos quedamos con el summary y el tipo del ticket
            String summary = body.get("issue").get("fields").get("summary").asText();
            String type = body.get("issue").get("fields").get("issuetype").get("name").asText();

            //Recuperamos el comentarios de los ticke para historico
            JsonNode history = helpDeskRestClient.getComments(key);
            TicketInfoDTO ticketInfoDTO = ticketInfoFactory.createCommentFromHistory(summary, type, history);
            logger.info("Processing comment from user by agent");

            triageTicketResponse(key, ticketInfoDTO);
        }catch (JsonProcessingException e){
            logger.severe("Error al procesar el nuevo comentario");
            throw new RuntimeException(e);
        }

    }

    public void diagnosticTicket(String key, JsonNode body) {
        try {
            logger.info(body.get("issue").get("fields").get("assignee").get("accountId").asText());
            //Nos quedamos con el summary y el tipo del ticket
            String summary = body.get("issue").get("fields").get("summary").asText();
            String type = body.get("issue").get("fields").get("issuetype").get("name").asText();

            //Recuperamos el comentarios de los ticke para historico
            JsonNode history = helpDeskRestClient.getComments(key);
            TicketInfoDTO ticketInfoDTO = ticketInfoFactory.createCommentFromHistory(summary, type, history);
            logger.info("Processing comment from user by agent");
            DiagnosticResponse diagnosticResponse = resolviaAgent.diagnosticTicket(mapper.writeValueAsString(ticketInfoDTO));
            List<String> texts = new ArrayList<>(List.of(diagnosticResponse.metadata().get("summary"), diagnosticResponse.metadata().get("evaluation"), diagnosticResponse.metadata().get("suggestions")));
            Comment comment = messagesFactory.createCommentMultiText(texts, true);
            helpDeskRestClient.addComment(key, comment);
        }catch (JsonProcessingException e){
            logger.severe("Error al procesar el diagnosticTicket");
            throw new RuntimeException(e);
        }
    }

    private void triageTicketResponse(String key, TicketInfoDTO ticketInfoDTO) throws JsonProcessingException {

        AgentTriageResponse agentTriageResponse = resolviaAgent.triage(mapper.writeValueAsString(ticketInfoDTO));
        sleepForRateExceded();

        //Si se recibe un numero de telefono
        if(agentTriageResponse.phone() != null && !agentTriageResponse.phone().isBlank()){
            try {
                PhoneCall phoneCall = PhoneCall.builder()
                        .maxDurationSeconds(1805)
                        .assistantId("17aa2a35-0f54-4136-8b0b-e15eca7d330c")
                        .phoneNumberId("2c6baf25-bfbf-401d-83fb-21c79df26634")
                        .customer(PhoneCall.Customer.builder().number(agentTriageResponse.phone()).build())
                        .build();
                agentPhoneRestClient.call(phoneCall);
                return;
            }catch (Exception e){
                Comment comment = messagesFactory.createComment("Se ha intentado llamar al cliente al número de telefono "+agentTriageResponse.phone() +"pero no ha podido establecerse la llamada por un error", true);
                helpDeskRestClient.addComment(key, comment);
                changeStateTicket(key, ESCALATED);
                logger.severe("Error al realizar la llamada");
            }
        }


        logger.info(Level.INFO + "Triage response: " + agentTriageResponse.response());
        switch (agentTriageResponse.type()) {
            case "closeTicket", "resolveTicket" -> {
                Comment comment = messagesFactory.createComment(agentTriageResponse.response(), false);
                helpDeskRestClient.addComment(key, comment);
                changeStateTicket(key, RESOLVE);
            }default -> {

                KnowledgeResponse knowledgeResponse = knowledgeRestClient.query(agentTriageResponse.ticketSummary());
                //si no hay información en la base de conocimiento
                if(knowledgeResponse.documents()==null || knowledgeResponse.documents().isEmpty()){
                    responseCustomer(resolviaAgent.desconocido(mapper.writeValueAsString(ticketInfoDTO)),key);
                    return;
                }
                AgentResponse agentResponse;
                //Si encontarmos información en la base de conocimiento que supera el umbral 0.9
                KnowledgeRequestLLM top = knowledgeResponse.documents().stream()
                        .filter(document -> Double.parseDouble(document.metadata().get("score")) >= 0.9)
                        .max(Comparator.comparingDouble(o -> Double.parseDouble(o.metadata().get("score"))))
                        .stream().findFirst().map(document -> KnowledgeRequestLLM.builder()
                                .ticketSummary(agentTriageResponse.ticketSummary())
                                .metadata(Map.of("template",document.metadata().get("template")))
                                .build()).orElse(null);

                if(top!=null){
                    agentResponse = resolviaAgent.generateResponseTop9(mapper.writeValueAsString(top));
                }else{
                    List<KnowledgeRequestLLM> posibles =
                            knowledgeResponse.documents().stream()
                                    .sorted(Comparator.comparingDouble(o -> Double.parseDouble(((KnowledgeResponse.Document)o).metadata().get("score"))).reversed())
                                    .map(document -> KnowledgeRequestLLM.builder()
                                            .ticketSummary(document.pageContent())
                                            .template(String.valueOf(document.metadata().get("template")))
                                            .build())
                                    .limit(3)
                                    .toList();
                    agentResponse = resolviaAgent.generateResponsePosibles(mapper.writeValueAsString(posibles));
                }
                responseCustomer(agentResponse,key);
            }
        }
    }

    private static void sleepForRateExceded() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void responseCustomer (AgentResponse agentResponse, String key){
         if(agentResponse.nextState().equalsIgnoreCase("ESCALAR")){
            Comment comment = messagesFactory.createComment(agentResponse.content(), true);
            helpDeskRestClient.addComment(key, comment);
            changeStateTicket(key, ESCALATED);
        }else if(agentResponse.nextState().equalsIgnoreCase("RESOLVER")){
            Comment comment = messagesFactory.createComment(agentResponse.content(), false);
            helpDeskRestClient.addComment(key, comment);
            changeStateTicket(key, RESOLVE);
        }else{
            changeStateTicket(key, ESCALATED);
        }
    }

    private void changeStateTicket(String key, String state) {
        try{
            //FIXME esto debería de ser robusto y antes que nada validar si el ticket puede realizar el cambio de estado
            //Controlar excepcion por lo menos para evitar que falle el sistema aunque no se cambie el estado del ticket
            helpDeskRestClient.transaction(key, State.builder().transition(new State.Transition(state)).build());
        }catch (Exception e){
            //FIXME crear excepción propia
            logger.severe("No ha sido posible realizar el cambio de estado del ticket");
        }
    }

    /**
     * Verifica si el autor del comentario es un agente de Atlassian que no sea el agente Resolvia
     *
     * @param body El nodo JSON que contiene la información del comentario.
     * @return true si el autor es un agente de Atlassian, false en caso contrario.
     */
    private boolean isAuthorAgent(JsonNode body) {
        logger.info(body.get("comment").get("author").get("accountType").asText());
        return body.get("comment").get("author").get("accountType").asText().equals("atlassian");
    }

    private boolean isAuthorCustomer(JsonNode body) {
        logger.info(body.get("comment").get("author").get("accountType").asText());
        return body.get("comment").get("author").get("accountType").asText().equals("customer") && !isAssigneeResolvia(body);
    }

    private boolean isAssigneeResolvia(JsonNode body) {
        logger.info(body.get("issue").get("fields").get("assignee").get("accountId").asText());
        return body.get("issue").get("fields").get("assignee").get("accountId").asText().equalsIgnoreCase(ID_ACCOUNT_RESOLVIA);
    }
}
