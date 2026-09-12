package com.dynatrace.easytrade.frauddetection.detector;

import com.dynatrace.easytrade.frauddetection.model.FraudAlert;
import com.dynatrace.easytrade.frauddetection.model.FraudType;
import com.dynatrace.easytrade.frauddetection.model.Severity;
import com.dynatrace.easytrade.frauddetection.model.Trade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Detects high-frequency trading: an abnormally large number of trades placed in a short window.
 * High velocity can indicate automated manipulation or account compromise.
 */
@Component
public class VelocityDetector {

    @Value("${fraud.rules.velocity.max-trades-per-hour}")
    private int maxTradesPerHour;

    public List<FraudAlert> detect(int accountId, String username, List<Trade> trades) {
        OffsetDateTime oneHourAgo = OffsetDateTime.now().minus(1, ChronoUnit.HOURS);

        long recentCount = trades.stream()
                .filter(t -> t.timestampOpen() != null && t.timestampOpen().isAfter(oneHourAgo))
                .count();

        if (recentCount <= maxTradesPerHour) {
            return Collections.emptyList();
        }

        double excess = (double) (recentCount - maxTradesPerHour) / maxTradesPerHour;
        Severity severity = excess > 2.0 ? Severity.CRITICAL : excess > 1.0 ? Severity.HIGH : Severity.MEDIUM;

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("description", String.format(
                "Account %d placed %d trades in the last hour (threshold: %d)",
                accountId, recentCount, maxTradesPerHour));
        evidence.put("tradesInLastHour", recentCount);
        evidence.put("threshold", maxTradesPerHour);
        evidence.put("excessMultiplier", Math.round(excess * 100.0) / 100.0);
        evidence.put("windowStart", oneHourAgo.toString());

        return List.of(FraudAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .fraudType(FraudType.HIGH_FREQUENCY_TRADING)
                .severity(severity)
                .accountId(accountId)
                .accountUsername(username)
                .instrumentId(null)
                .evidence(evidence)
                .score(Math.min(1.0, 0.4 + (excess * 0.3)))
                .build());
    }
}
