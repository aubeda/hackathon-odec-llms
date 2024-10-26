package es.hackathon.model;

import lombok.Builder;

@Builder
public record LlmResponse (String role, boolean suggestion, String nextState, String content) {
}
