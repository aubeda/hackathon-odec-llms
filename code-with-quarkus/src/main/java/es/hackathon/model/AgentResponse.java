package es.hackathon.model;

import lombok.Builder;

@Builder
public record AgentResponse(String role, Boolean suggestion, String nextState, String content) {
}
