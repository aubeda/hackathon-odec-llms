package es.hackathon.model;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record KnowledgeRequestLLM(String ticketSummary, String template, Map<String,String> metadata) {
}
