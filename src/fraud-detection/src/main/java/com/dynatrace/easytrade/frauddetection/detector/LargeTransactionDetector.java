package com.dynatrace.easytrade.frauddetection.detector;

import com.dynatrace.easytrade.frauddetection.model.FraudAlert;
import com.dynatrace.easytrade.frauddetection.model.FraudType;
import com.dynatrace.easytrade.frauddetection.model.Severity;
import com.dynatrace.easytrade.frauddetection.model.Trade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects anomalously large individual transactions relative to the account's historical average.
 * A single trade significantly above average may indicate account takeover or market manipulation.
 */
@Component
public class LargeTransactionDetector {

    @Value("${fraud.rules.large-transaction.threshold-multiplier}")
    private double thresholdMultiplier;

    public List<FraudAlert> detect(int accountId, String username, List<Trade> trades) {
        if (trades.size() < 3) {
            return Collections.emptyList();
        }

        List<Double> values = trades.stream()
                .map(Trade::totalValue)
                .filter(v -> v > 0)
                .collect(Collectors.toList());

        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double threshold = average * thresholdMultiplier;

        List<FraudAlert> alerts = new ArrayList<>();
        List<Trade> recent = trades.stream()
                .filter(t -> t.timestampOpen() != null)
                .sorted(Comparator.comparing(Trade::timestampOpen).reversed())
                .limit(20)
                .toList();

        for (Trade trade : recent) {
            double value = trade.totalValue();
            if (value > threshold && value > 0) {
                double ratio = value / average;
                Severity severity = ratio > 10 ? Severity.CRITICAL : ratio > 5 ? Severity.HIGH : Severity.MEDIUM;

                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("description", String.format(
                        "Trade value %.2f is %.1fx the account average of %.2f",
                        value, ratio, average));
                evidence.put("tradeValue", Math.round(value * 100.0) / 100.0);
                evidence.put("accountAverageValue", Math.round(average * 100.0) / 100.0);
                evidence.put("ratio", Math.round(ratio * 100.0) / 100.0);
                evidence.put("threshold", Math.round(threshold * 100.0) / 100.0);
                evidence.put("direction", trade.direction());
                evidence.put("quantity", trade.quantity());
                evidence.put("entryPrice", trade.entryPrice());
                evidence.put("tradeTimestamp", trade.timestampOpen() != null ? trade.timestampOpen().toString() : "unknown");

                alerts.add(FraudAlert.builder()
                        .alertId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .fraudType(FraudType.LARGE_TRANSACTION)
                        .severity(severity)
                        .accountId(accountId)
                        .accountUsername(username)
                        .instrumentId(trade.instrumentId())
                        .evidence(evidence)
                        .score(Math.min(1.0, 0.3 + (ratio / 20.0)))
                        .build());
                break; // one alert per analysis cycle for this account
            }
        }

        return alerts;
    }
}
