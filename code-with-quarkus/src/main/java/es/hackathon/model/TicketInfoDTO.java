package es.hackathon.model;

import lombok.Builder;

import java.util.List;

@Builder
public record TicketInfoDTO(String summary, String type, List<Message> messages, Integer eval) {

    @Builder
    public record Message(String role, String content) {
    }
}
