package com.dynatrace.easytrade.frauddetection.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Price(
        Integer id,
        Integer instrumentId,
        String timestamp,
        Double open,
        Double high,
        Double low,
        Double close
) {
    public double midpoint() {
        if (high != null && low != null) {
            return (high + low) / 2.0;
        }
        return close != null ? close : 0.0;
    }
}
