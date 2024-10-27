package es.hackathon.model;

import lombok.Builder;

@Builder
public record PhoneCall(Integer maxDurationSeconds, String assistantId,String phoneNumberId, Customer customer) {

    @Builder
    public record Customer(String number) {}
}
