package es.hackathon.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.rest.client.reactive.ClientBasicAuth;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import es.hackathon.model.Comment;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "helpdesk")
@ClientBasicAuth(username = "${helpdesk.username}", password = "${helpdesk.password}")
public interface HelpDeskRestClient {

    @POST
    @Path("/issue/{key}/comment")
    void addComment(@PathParam("key") String key, Comment comment);

    @GET
    @Path("/issue/{key}")
    JsonNode getTicket(@PathParam("key") String key);
}
