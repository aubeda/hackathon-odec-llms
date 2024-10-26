package model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
public record Comment(Body body, List<Property> properties, Object visibility) {

    @Builder
    public record Body(int version, String type, List<Content> content) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(String type, List<Content> content, String text) {}

    @Builder
    public record Property(String key, Value value) {}

    @Builder
    public record Value(boolean internal) {}
}