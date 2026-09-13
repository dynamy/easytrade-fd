package com.dynatrace.easytrade.frauddetection.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Trade(
        Integer instrumentId,
        String direction,
        Double quantity,
        Double entryPrice,
        OffsetDateTime timestampOpen,
        OffsetDateTime timestampClose,
        Boolean tradeClosed,
        Boolean transactionHappened,
        String status
) {
    public boolean isBuy() {
        return "buy".equalsIgnoreCase(direction);
    }

    public boolean isSell() {
        return "sell".equalsIgnoreCase(direction);
    }

    public boolean isLong() {
        return Boolean.FALSE.equals(tradeClosed) && Boolean.FALSE.equals(transactionHappened);
    }

    public boolean isQuick() {
        return Boolean.TRUE.equals(tradeClosed) && Boolean.TRUE.equals(transactionHappened);
    }

    public double totalValue() {
        double q = quantity != null ? quantity : 0.0;
        double p = entryPrice != null ? entryPrice : 0.0;
        return q * p;
    }
}
