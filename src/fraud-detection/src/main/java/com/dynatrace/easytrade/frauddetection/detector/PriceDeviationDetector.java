package com.dynatrace.easytrade.frauddetection.detector;

import com.dynatrace.easytrade.frauddetection.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Detects trades executed at prices significantly deviating from the current market price.
 * Large deviations can indicate front-running, insider trading, or off-market manipulation.
 */
@Component
public class PriceDeviationDetector {

    @Value("${fraud.rules.price-deviation.max-percent}")
    private double maxDeviationPercent;

    public List<FraudAlert> detect(int accountId, String username, List<Trade> trades,
                                   Map<Integer, Price> latestPrices,
                                   Map<Integer, String> instrumentNames) {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(2, ChronoUnit.HOURS);
        List<FraudAlert> alerts = new ArrayList<>();

        List<Trade> recentTrades = trades.stream()
                .filter(t -> t.timestampOpen() != null && t.timestampOpen().isAfter(cutoff))
                .filter(t -> t.entryPrice() != null && t.entryPrice() > 0)
                .toList();

        for (Trade trade : recentTrades) {
            Price currentPrice = latestPrices.get(trade.instrumentId());
            if (currentPrice == null || currentPrice.close() == null || currentPrice.close() == 0) {
                continue;
            }

            double marketPrice = currentPrice.close();
            double deviation = Math.abs(trade.entryPrice() - marketPrice) / marketPrice * 100.0;

            if (deviation > maxDeviationPercent) {
                Severity severity = deviation > maxDeviationPercent * 3 ? Severity.CRITICAL
                        : deviation > maxDeviationPercent * 1.5 ? Severity.HIGH
                        : Severity.MEDIUM;

                String instrumentName = instrumentNames.getOrDefault(trade.instrumentId(),
                        "Instrument " + trade.instrumentId());

                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("description", String.format(
                        "Trade on %s at %.4f deviates %.2f%% from market price %.4f",
                        instrumentName, trade.entryPrice(), deviation, marketPrice));
                evidence.put("tradeEntryPrice", trade.entryPrice());
                evidence.put("currentMarketPrice", marketPrice);
                evidence.put("deviationPercent", Math.round(deviation * 100.0) / 100.0);
                evidence.put("thresholdPercent", maxDeviationPercent);
                evidence.put("direction", trade.direction());
                evidence.put("tradeTimestamp", trade.timestampOpen().toString());

                alerts.add(FraudAlert.builder()
                        .alertId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .fraudType(FraudType.PRICE_DEVIATION)
                        .severity(severity)
                        .accountId(accountId)
                        .accountUsername(username)
                        .instrumentId(trade.instrumentId())
                        .instrumentName(instrumentName)
                        .evidence(evidence)
                        .score(Math.min(1.0, deviation / (maxDeviationPercent * 5)))
                        .build());
                break; // one alert per cycle
            }
        }

        return alerts;
    }
}
