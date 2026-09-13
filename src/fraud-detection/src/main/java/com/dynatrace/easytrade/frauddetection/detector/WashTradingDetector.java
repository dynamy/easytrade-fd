package com.dynatrace.easytrade.frauddetection.detector;

import com.dynatrace.easytrade.frauddetection.model.FraudAlert;
import com.dynatrace.easytrade.frauddetection.model.FraudType;
import com.dynatrace.easytrade.frauddetection.model.Severity;
import com.dynatrace.easytrade.frauddetection.model.Trade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects wash trading: buying and selling the same instrument within a short window.
 * Wash trading inflates volume metrics and can be used to manipulate prices.
 */
@Component
public class WashTradingDetector {

    @Value("${fraud.rules.wash-trading.window-minutes}")
    private int windowMinutes;

    public List<FraudAlert> detect(int accountId, String username, List<Trade> trades) {
        List<FraudAlert> alerts = new ArrayList<>();

        Map<Integer, List<Trade>> byInstrument = trades.stream()
                .filter(t -> t.timestampOpen() != null)
                .collect(Collectors.groupingBy(Trade::instrumentId));

        for (Map.Entry<Integer, List<Trade>> entry : byInstrument.entrySet()) {
            int instrumentId = entry.getKey();
            List<Trade> instrumentTrades = entry.getValue();

            List<Trade> buys = instrumentTrades.stream().filter(Trade::isBuy).toList();
            List<Trade> sells = instrumentTrades.stream().filter(Trade::isSell).toList();

            for (Trade buy : buys) {
                for (Trade sell : sells) {
                    long diffMinutes = Math.abs(
                            buy.timestampOpen().toEpochSecond() - sell.timestampOpen().toEpochSecond()
                    ) / 60;

                    if (diffMinutes <= windowMinutes) {
                        double score = 1.0 - (diffMinutes / (double) windowMinutes);
                        double totalValue = buy.totalValue() + sell.totalValue();

                        Map<String, Object> evidence = new LinkedHashMap<>();
                        evidence.put("description", String.format(
                                "Account %d bought and sold instrument %d within %d minutes",
                                accountId, instrumentId, diffMinutes));
                        evidence.put("buyTimestamp", formatTimestamp(buy.timestampOpen()));
                        evidence.put("sellTimestamp", formatTimestamp(sell.timestampOpen()));
                        evidence.put("timeDifferenceMinutes", diffMinutes);
                        evidence.put("buyQuantity", buy.quantity());
                        evidence.put("sellQuantity", sell.quantity());
                        evidence.put("buyEntryPrice", buy.entryPrice());
                        evidence.put("sellEntryPrice", sell.entryPrice());
                        evidence.put("combinedValue", totalValue);

                        alerts.add(FraudAlert.builder()
                                .alertId(UUID.randomUUID().toString())
                                .timestamp(Instant.now())
                                .fraudType(FraudType.WASH_TRADING)
                                .severity(diffMinutes <= 10 ? Severity.CRITICAL : Severity.HIGH)
                                .accountId(accountId)
                                .accountUsername(username)
                                .instrumentId(instrumentId)
                                .evidence(evidence)
                                .score(Math.min(1.0, score + 0.3))
                                .build());
                        break; // one alert per instrument per buy
                    }
                }
            }
        }

        return alerts;
    }

    private String formatTimestamp(OffsetDateTime dt) {
        return dt != null ? dt.toString() : "unknown";
    }
}
