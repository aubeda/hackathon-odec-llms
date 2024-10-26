package es.hackathon.agent;

import com.fasterxml.jackson.databind.JsonNode;
import es.hackathon.factory.MessagesFactory;
import es.hackathon.model.Comment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.logging.Logger;

@ApplicationScoped
public class AgentService {

   private static Logger logger = Logger.getLogger(AgentService.class.getName());

    @RestClient
    HelpDeskRestClient helpDeskRestClient;

    @Inject
    MessagesFactory messagesFactory;

    public void resolveNewTicket(String key, JsonNode body) {
        //TODO llamar al agente para elaborar una respuesta con IA
        //TODO añadir el cambio de estado del ticket para ponerlo en progreso
        Comment comment = messagesFactory.createComment("hola pepe que tal estas", false);
        helpDeskRestClient.addComment(key, comment);
    }

    public void resolveCommentAgent(String key, JsonNode body) {
        if(isAgentAtlassian(body)){
            logger.info("Ignoring comment from atlassian");
            return;
        }
        logger.info("Processing comment from user by agent");
        //TODO lamar al agente para elaborar una respuesta con IA
        //TODO añadir el cambio de estado del ticket reabrirlo si no se ha resuetlo el problema y cerrarlo en caso de que si
        Comment comment = messagesFactory.createComment("hola pepe que tal estas", false);
        helpDeskRestClient.addComment(key, comment);
    }

    /**
     * Verifica si el autor del comentario es un agente de Atlassian.
     *
     * @param body El nodo JSON que contiene la información del comentario.
     * @return true si el autor es un agente de Atlassian, false en caso contrario.
     */
    private static boolean isAgentAtlassian(JsonNode body) {
        return body.get("comment").get("author").get("accountType").asText().equals("atlassian");
    }

    public void cannotResolverAgent(String key, JsonNode body) {
        Comment comment = messagesFactory.createComment("hola pepe que tal estas", true);
        helpDeskRestClient.addComment(key, comment);
    }

//    private boolean shouldActOnComment(String key) {
//        Comment comment = helpDeskRestClient.getComment(key);
//        return comment != null && !comment.isInternal();
//    }
}
