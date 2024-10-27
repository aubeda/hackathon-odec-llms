package es.hackathon.model;

import lombok.Builder;

@Builder
public record AgentTriageResponse(String ticketSummary, String type, String response, String phone) {
}
