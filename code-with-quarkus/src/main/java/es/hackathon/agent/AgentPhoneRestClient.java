package es.hackathon.agent;

import es.hackathon.model.PhoneCall;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "phone-api")
@ClientHeaderParam(name = "Authorization", value = "Bearer 11699b6c-d8c4-458f-88a5-7bf5acb14166")
public interface AgentPhoneRestClient {

    @POST
    @Path("/call/phone")
    void call(PhoneCall phoneCall);
}
