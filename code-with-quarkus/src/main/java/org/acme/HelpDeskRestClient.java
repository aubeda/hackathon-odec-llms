package org.acme;

import io.quarkus.rest.client.reactive.ClientBasicAuth;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import model.Comment;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "helpdesk")
@ClientBasicAuth(username = "${helpdesk.username}", password = "${helpdesk.password}")
public interface HelpDeskRestClient {

    @POST
    @Path("/issue/{key}/comment")
    void addComment(@PathParam("key") String key, Comment comment);
}
