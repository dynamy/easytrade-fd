package com.dynatrace.easytrade.frauddetection.detector;

import com.dynatrace.easytrade.frauddetection.model.FraudAlert;
import com.dynatrace.easytrade.frauddetection.model.FraudType;
import com.dynatrace.easytrade.frauddetection.model.Severity;
import com.dynatrace.easytrade.frauddetection.model.Trade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects trades placed during unusual hours (e.g., 00:00–04:00 UTC).
 * Trading at off-hours may indicate bot activity or account compromise.
 */
@Component
public class UnusualHoursDetector {

    @Value("${fraud.rules.unusual-hours.utc-start-hour}")
    private int startHour;

    @Value("${fraud.rules.unusual-hours.utc-end-hour}")
    private int endHour;

    public List<FraudAlert> detect(int accountId, String username, List<Trade> trades) {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minus(1, ChronoUnit.HOURS);

        List<Trade> unusualTrades = trades.stream()
                .filter(t -> t.timestampOpen() != null && t.timestampOpen().isAfter(cutoff))
                .filter(t -> isUnusualHour(t.timestampOpen()))
                .collect(Collectors.toList());

        if (unusualTrades.isEmpty()) {
            return Collections.emptyList();
        }

        List<FraudAlert> alerts = new ArrayList<>();
        for (Trade trade : unusualTrades) {
            int hour = trade.timestampOpen().withOffsetSameInstant(ZoneOffset.UTC).getHour();

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("description", String.format(
                    "Trade placed at %02d:%02d UTC, outside normal trading hours (%02d:00–%02d:00)",
                    hour, trade.timestampOpen().getMinute(), startHour, endHour));
            evidence.put("tradeTimestampUtc", trade.timestampOpen().withOffsetSameInstant(ZoneOffset.UTC).toString());
            evidence.put("tradeHourUtc", hour);
            evidence.put("unusualWindowStart", String.format("%02d:00 UTC", startHour));
            evidence.put("unusualWindowEnd", String.format("%02d:00 UTC", endHour));
            evidence.put("direction", trade.direction());
            evidence.put("instrumentId", trade.instrumentId());
            evidence.put("tradeValue", Math.round(trade.totalValue() * 100.0) / 100.0);

            alerts.add(FraudAlert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .fraudType(FraudType.UNUSUAL_HOURS_TRADING)
                    .severity(Severity.MEDIUM)
                    .accountId(accountId)
                    .accountUsername(username)
                    .instrumentId(trade.instrumentId())
                    .evidence(evidence)
                    .score(0.55)
                    .build());
        }

        return alerts;
    }

    private boolean isUnusualHour(OffsetDateTime dt) {
        int hour = dt.withOffsetSameInstant(ZoneOffset.UTC).getHour();
        return hour >= startHour && hour < endHour;
    }
}
