package es.hackathon.agent;

import com.fasterxml.jackson.databind.JsonNode;
import es.hackathon.model.State;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logmanager.Level;
import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

@Path("/agent")
public class HelpdeskResource {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HelpdeskResource.class);
    Logger logger = Logger.getLogger(HelpdeskResource.class.getName());

    @Inject
    HelpdeskService agentService;

    /**
     * Este método maneja la respuesta a la de un nuevo ticket en el sistema de helpdesk.
     * Toma una clave como parámetro y delega la resolución del ticket
     * al agentService.
     *
     * @param key el identificador del ticket a ser resuelto
     * @param body el contenido enviado por el sistema de soporte
     * @return una respuesta que indica el resultado de la operación
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/helpdesk/{key}")
    public Response webhookNewTicket(@PathParam("key") String key, JsonNode body) {
//        logger.log(Level.DEBUG, body.toPrettyString());
        agentService.newTicketCreated(key, body);
        return Response.ok().build();
    }

    /**
     * Este método maneja la respuesta a un comentario del cliente en un ticket existente en
     * el sistema de soporte. Toma una clave como parámetro y delega la
     * resolución del comentario al agentService.
     *
     * @param key el identificador del ticket cuyo comentario se va a resolver
     * @param body el contenido enviado por el sistema de soporte
     * @return una Respuesta que indica el resultado de la operación
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/helpdesk/{key}/comment")
    public Response webhookAddComment(@PathParam("key") String key, JsonNode body) {
//        logger.log(Level.DEBUG, body.toPrettyString());
        agentService.newCommentInTicket(key, body);
        return Response.ok().build();
    }

    /**
     * Este método maneja la respuesta a un comentario del cliente en un ticket existente en
     * el sistema de soporte. Toma una clave como parámetro y delega la
     * resolución del comentario al agentService.
     *
     * @param key el identificador del ticket cuyo comentario se va a resolver
     * @param body el contenido enviado por el sistema de soporte
     * @return una Respuesta que indica el resultado de la operación
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/helpdesk/{key}/diagnostic")
    public Response webhookDiagnostic(@PathParam("key") String key, JsonNode body) {
//        logger.log(Level.DEBUG, body.toPrettyString());
        agentService.diagnosticTicket(key, body);
        return Response.ok().build();
    }

}