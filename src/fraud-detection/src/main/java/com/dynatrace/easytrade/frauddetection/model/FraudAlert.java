package com.dynatrace.easytrade.frauddetection.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class FraudAlert {

    private String alertId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    private FraudType fraudType;
    private Severity severity;

    private Integer accountId;
    private String accountUsername;

    private Integer instrumentId;
    private String instrumentName;

    private Map<String, Object> evidence;

    /** Confidence score from 0.0 (uncertain) to 1.0 (certain). */
    private Double score;

    public String deduplicationKey() {
        return fraudType + ":" + accountId + ":" + instrumentId;
    }
}
