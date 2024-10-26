package es.hackathon.agent;

import com.fasterxml.jackson.databind.JsonNode;
import es.hackathon.model.Comment;
import es.hackathon.model.KnowledgeResponse;
import es.hackathon.model.State;
import io.quarkus.rest.client.reactive.ClientBasicAuth;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "knowledge-api")
public interface KnowledgeRestClient {

    @POST
    @Path("/query")
    KnowledgeResponse query(String query);
}
