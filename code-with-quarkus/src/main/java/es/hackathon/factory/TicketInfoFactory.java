package es.hackathon.factory;

import com.fasterxml.jackson.databind.JsonNode;
import es.hackathon.model.TicketInfoDTO;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class TicketInfoFactory {


    public TicketInfoDTO createCommentFromNewTicket(JsonNode jsonNewTicket) {
        return TicketInfoDTO.builder()
                .summary(jsonNewTicket.path("issue").path("fields").path("summary").asText())
                .type(jsonNewTicket.path("issue").path("fields").path("issuetype").path("name").asText())
                .messages(List.of(TicketInfoDTO.Message.builder()
                        .role("user")
                        .content(jsonNewTicket.path("issue").path("fields").path("description").asText())
                        .build()))
                .build();
    }

    public TicketInfoDTO createCommentFromHistory(String summary, String type, JsonNode comments) {
        return TicketInfoDTO.builder()
                .summary(summary)
                .type(type)
                .messages(
                        StreamSupport.stream(comments.get("comments").spliterator(), false)
                                .map(comment -> TicketInfoDTO.Message.builder()
                                        .role(getRole(comment))
                                        .content(comment.path("body").path("content").get(0).path("content").get(0).path("text").asText())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static @NotNull String getRole(JsonNode comment) {
        String accountType = comment.path("author").path("accountType").asText();
        String accountId = comment.path("author").path("accountId").asText();

        return switch (accountType) {
            case "customer" -> "user";
            case "712020:a597a544-8ead-450a-aa72-6eb6cd826cf5" -> "assistantIA";
            default -> "assistantHuman";
        };
    }
}
