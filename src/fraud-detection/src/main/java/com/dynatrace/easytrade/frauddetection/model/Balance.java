package com.dynatrace.easytrade.frauddetection.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Balance(
        Integer accountId,
        Double value
) {}
