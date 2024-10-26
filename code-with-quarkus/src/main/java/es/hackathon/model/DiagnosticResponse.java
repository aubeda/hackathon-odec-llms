package es.hackathon.model;

import lombok.Builder;

import java.util.Map;

@Builder
public record DiagnosticResponse(String ticketSummary, String template, Map<String,String> metadata) {
}
