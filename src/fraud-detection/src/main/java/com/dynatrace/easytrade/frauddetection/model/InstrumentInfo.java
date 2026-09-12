package com.dynatrace.easytrade.frauddetection.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InstrumentInfo(
        Integer id,
        String code,
        String name,
        String description,
        Integer productId,
        String productName,
        Price price,
        Double amount
) {}
