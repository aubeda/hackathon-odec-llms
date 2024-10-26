package es.hackathon.model;

import lombok.Builder;

@Builder
public record State(Transition transition) {

    public record Transition(String id) {}
}
