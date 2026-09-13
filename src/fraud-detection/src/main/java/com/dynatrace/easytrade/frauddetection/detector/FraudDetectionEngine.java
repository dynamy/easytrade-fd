package com.dynatrace.easytrade.frauddetection.detector;

import com.dynatrace.easytrade.frauddetection.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates all fraud detectors and aggregates their alerts for a single account.
 */
@Component
public class FraudDetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionEngine.class);

    private final WashTradingDetector washTradingDetector;
    private final VelocityDetector velocityDetector;
    private final LargeTransactionDetector largeTransactionDetector;
    private final UnusualHoursDetector unusualHoursDetector;
    private final PriceDeviationDetector priceDeviationDetector;
    private final ExcessiveLongPositionDetector excessiveLongPositionDetector;

    public FraudDetectionEngine(
            WashTradingDetector washTradingDetector,
            VelocityDetector velocityDetector,
            LargeTransactionDetector largeTransactionDetector,
            UnusualHoursDetector unusualHoursDetector,
            PriceDeviationDetector priceDeviationDetector,
            ExcessiveLongPositionDetector excessiveLongPositionDetector) {
        this.washTradingDetector = washTradingDetector;
        this.velocityDetector = velocityDetector;
        this.largeTransactionDetector = largeTransactionDetector;
        this.unusualHoursDetector = unusualHoursDetector;
        this.priceDeviationDetector = priceDeviationDetector;
        this.excessiveLongPositionDetector = excessiveLongPositionDetector;
    }

    public List<FraudAlert> analyze(
            Account account,
            List<Trade> recentTrades,
            List<Trade> openLongTrades,
            Map<Integer, Price> latestPrices,
            List<InstrumentInfo> instruments) {

        int accountId = account.id();
        String username = account.displayName();

        Map<Integer, String> instrumentNames = instruments.stream()
                .filter(i -> i.id() != null && i.name() != null)
                .collect(Collectors.toMap(InstrumentInfo::id, InstrumentInfo::name, (a, b) -> a));

        log.debug("Analyzing account {} ({}) with {} trades, {} open longs, {} instruments",
                accountId, username, recentTrades.size(), openLongTrades.size(), instruments.size());

        List<FraudAlert> alerts = new ArrayList<>();

        try {
            alerts.addAll(washTradingDetector.detect(accountId, username, recentTrades));
        } catch (Exception e) {
            log.error("WashTradingDetector failed for account {}: {}", accountId, e.getMessage());
        }

        try {
            alerts.addAll(velocityDetector.detect(accountId, username, recentTrades));
        } catch (Exception e) {
            log.error("VelocityDetector failed for account {}: {}", accountId, e.getMessage());
        }

        try {
            alerts.addAll(largeTransactionDetector.detect(accountId, username, recentTrades));
        } catch (Exception e) {
            log.error("LargeTransactionDetector failed for account {}: {}", accountId, e.getMessage());
        }

        try {
            alerts.addAll(unusualHoursDetector.detect(accountId, username, recentTrades));
        } catch (Exception e) {
            log.error("UnusualHoursDetector failed for account {}: {}", accountId, e.getMessage());
        }

        try {
            alerts.addAll(priceDeviationDetector.detect(accountId, username, recentTrades,
                    latestPrices, instrumentNames));
        } catch (Exception e) {
            log.error("PriceDeviationDetector failed for account {}: {}", accountId, e.getMessage());
        }

        try {
            alerts.addAll(excessiveLongPositionDetector.detect(accountId, username,
                    openLongTrades, instrumentNames));
        } catch (Exception e) {
            log.error("ExcessiveLongPositionDetector failed for account {}: {}", accountId, e.getMessage());
        }

        if (!alerts.isEmpty()) {
            log.info("Account {} ({}): detected {} fraud signal(s): {}",
                    accountId, username, alerts.size(),
                    alerts.stream().map(a -> a.getFraudType().name()).collect(Collectors.joining(", ")));
        }

        return alerts;
    }
}
