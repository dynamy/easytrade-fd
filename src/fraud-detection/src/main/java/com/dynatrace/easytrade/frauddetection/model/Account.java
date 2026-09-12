package com.dynatrace.easytrade.frauddetection.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Account(
        Integer id,
        Integer packageId,
        String firstName,
        String lastName,
        String username,
        String email,
        String origin,
        String address,
        Boolean accountActive
) {
    public String displayName() {
        return username != null ? username : (firstName + " " + lastName);
    }
}
