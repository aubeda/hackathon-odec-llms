package org.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.Comment;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@Path("/agent")
public class AgentResource {

    @RestClient
    HelpDeskRestClient helpDeskRestClient;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/helpdesk/ticket/{key}")
    public Response webhookNewTicket(@PathParam("key") String key) throws JsonProcessingException {

        Comment comment = Comment.builder()
                .body(new Comment.Body(1, "doc", List.of(
                        new Comment.Content("paragraph", List.of(
                                new Comment.Content("text", null, "hola pepe que tal estas")
                        ), null)
                )))
                .properties(List.of(
                        new Comment.Property("sd.public.comment", new Comment.Value(true))
                ))
                .visibility(null)
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(comment));
        helpDeskRestClient.addComment(key, comment);
        return Response.ok().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/helpdesk/ticket/{key}/comment")
    public Response webhookUpdateTicket(@PathParam("key") String key) {

        Comment comment = Comment.builder()
                .body(new Comment.Body(1, "doc", List.of(
                        new Comment.Content("paragraph", List.of(
                                new Comment.Content("text", null, "evento autulizado")
                        ), null)
                )))
                .properties(List.of(
                        new Comment.Property("sd.public.comment", new Comment.Value(true))
                ))
                .visibility(null)
                .build();

        System.out.println(comment);
        helpDeskRestClient.addComment(key, comment);
        return Response.ok().build();
    }
}
