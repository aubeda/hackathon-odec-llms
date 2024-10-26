package es.hackathon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record KnowledgeResponse(List<Document> documents) {

    @Builder
    public record Document(@JsonProperty("page_content")String pageContent, Map<String,String> metadata) {
    }
}
