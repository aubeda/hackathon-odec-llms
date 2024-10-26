package es.hackathon.agent;

import com.fasterxml.jackson.databind.JsonNode;
import es.hackathon.agent.ia.ResolviaAgent;
import es.hackathon.factory.HelpdeskResponseFactory;
import es.hackathon.factory.TicketInfoFactory;
import es.hackathon.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class HelpdeskService {

    public static final String EN_CURSO = "891";
    public static final String RESOLVE = "761";
    public static final String ESCALATED = "921";
    public static final String ID_ACCOUNT_RESOLVIA = "712020:a597a544-8ead-450a-aa72-6eb6cd826cf5";

    private static Logger logger = Logger.getLogger(HelpdeskService.class.getName());

    @Inject
    @RestClient
    HelpdeskRestClient helpDeskRestClient;
    @Inject
    HelpdeskResponseFactory messagesFactory;
    @Inject
    TicketInfoFactory ticketInfoFactory;
    @Inject
    ResolviaAgent resolviaAgent;

    public void newTicketCreated(String key, JsonNode body) {
        TicketInfoDTO ticketInfoDTO = ticketInfoFactory.createCommentFromNewTicket(body);
        triageTicketResponse(key, ticketInfoDTO);
    }

    private void triageTicketResponse(String key, TicketInfoDTO ticketInfoDTO) {
        AgentTriageResponse agentTriageResponse = resolviaAgent.triage(ticketInfoDTO);
        logger.info(Level.INFO + "Triage response: " + agentTriageResponse.response());
        switch (agentTriageResponse.type()) {
            case "closeTicket", "resolveTicket" -> {
                Comment comment = messagesFactory.createComment(agentTriageResponse.response(), false);
                helpDeskRestClient.addComment(key, comment);
                changeStateTicket(key, RESOLVE);
            }
            default -> {
                //TODO API VICENTE  //desconocido, guia, reserva para base de conocimiento
                //TODO lamar al agente para elaborar una respuesta con IA
                //TODO añadir el cambio de estado del ticket reabrirlo si no se ha resuetlo el problema y cerrarlo en caso de que si
                //        LlmResponse llmResponse = LlmResponse.builder().build();
                //        if(llmResponse.nextState().equalsIgnoreCase("ESCALAR")){
                //            Comment comment = messagesFactory.createComment("hola pepe que tal estas", true);
                //            helpDeskRestClient.addComment(key, comment);
                //            changeStateTicket(key, ESCALATED);
                //        }else if(llmResponse.nextState().equalsIgnoreCase("RESOLVER")){
                //            Comment comment = messagesFactory.createComment("hola pepe que tal estas", false);
                //            helpDeskRestClient.addComment(key, comment);
                //            changeStateTicket(key, RESOLVE);
                //        }else{
                //            changeStateTicket(key, ESCALATED);
                //        }
                changeStateTicket(key, EN_CURSO);
            }
        }
    }

    public void newCommentInTicket(String key, JsonNode body) {

        if(!isAssigneeResolvia(body) || isAuthorAgent(body)){
            logger.info("Ignoring comment from atlassian");
            return;
        }

        //Nos quedamos con el summary y el tipo del ticket
        String summary = body.get("issue").get("fields").get("summary").asText();
        String type = body.get("issue").get("fields").get("issuetype").get("name").asText();

        //Recuperamos el comentarios de los ticke para historico
        JsonNode history = helpDeskRestClient.getComments(key);
        TicketInfoDTO ticketInfoDTO = ticketInfoFactory.createCommentFromHistory(summary,type, history);
        logger.info("Processing comment from user by agent");

        triageTicketResponse(key, ticketInfoDTO);

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




//    public void cannotResolverAgent(String key, JsonNode body) {
//        Comment comment = messagesFactory.createComment("hola pepe que tal estas", true);
//        helpDeskRestClient.addComment(key, comment);
//    }

//    public void transitionTicket(String key, State transition) {
//        helpDeskRestClient.transaction(key, transition);
//    }

//    private boolean shouldActOnComment(String key) {
//        Comment comment = helpDeskRestClient.getComment(key);
//        return comment != null && !comment.isInternal();
//    }
}
