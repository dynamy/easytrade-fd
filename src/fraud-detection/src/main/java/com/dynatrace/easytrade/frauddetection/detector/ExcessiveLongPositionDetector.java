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
 * Detects excessive open long positions in a single instrument.
 * Stacking many open long orders (layering) can create artificial demand signals.
 */
@Component
public class ExcessiveLongPositionDetector {

    @Value("${fraud.rules.long-position.max-open-per-instrument}")
    private int maxOpenPerInstrument;

    public List<FraudAlert> detect(int accountId, String username, List<Trade> openLongTrades,
                                   Map<Integer, String> instrumentNames) {
        Map<Integer, List<Trade>> byInstrument = openLongTrades.stream()
                .filter(Trade::isLong)
                .collect(Collectors.groupingBy(Trade::instrumentId));

        List<FraudAlert> alerts = new ArrayList<>();

        for (Map.Entry<Integer, List<Trade>> entry : byInstrument.entrySet()) {
            int instrumentId = entry.getKey();
            List<Trade> positions = entry.getValue();

            if (positions.size() > maxOpenPerInstrument) {
                int excess = positions.size() - maxOpenPerInstrument;
                Severity severity = excess > maxOpenPerInstrument ? Severity.CRITICAL
                        : excess > maxOpenPerInstrument / 2 ? Severity.HIGH
                        : Severity.MEDIUM;

                double totalQuantity = positions.stream()
                        .mapToDouble(t -> t.quantity() != null ? t.quantity() : 0)
                        .sum();
                double totalValue = positions.stream()
                        .mapToDouble(Trade::totalValue)
                        .sum();

                String instrumentName = instrumentNames.getOrDefault(instrumentId,
                        "Instrument " + instrumentId);

                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("description", String.format(
                        "Account %d has %d open long positions in %s (threshold: %d)",
                        accountId, positions.size(), instrumentName, maxOpenPerInstrument));
                evidence.put("openLongPositions", positions.size());
                evidence.put("threshold", maxOpenPerInstrument);
                evidence.put("totalQuantity", Math.round(totalQuantity * 100.0) / 100.0);
                evidence.put("totalExposureValue", Math.round(totalValue * 100.0) / 100.0);
                evidence.put("instrumentName", instrumentName);

                alerts.add(FraudAlert.builder()
                        .alertId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .fraudType(FraudType.EXCESSIVE_LONG_POSITION)
                        .severity(severity)
                        .accountId(accountId)
                        .accountUsername(username)
                        .instrumentId(instrumentId)
                        .instrumentName(instrumentName)
                        .evidence(evidence)
                        .score(Math.min(1.0, 0.4 + (excess / (double) maxOpenPerInstrument) * 0.4))
                        .build());
            }
        }

        return alerts;
    }
}
