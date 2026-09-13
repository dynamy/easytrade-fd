package com.dynatrace.easytrade.frauddetection.scheduler;

import com.dynatrace.easytrade.frauddetection.client.AccountServiceClient;
import com.dynatrace.easytrade.frauddetection.client.BrokerServiceClient;
import com.dynatrace.easytrade.frauddetection.client.PricingServiceClient;
import com.dynatrace.easytrade.frauddetection.detector.FraudDetectionEngine;
import com.dynatrace.easytrade.frauddetection.model.*;
import com.dynatrace.easytrade.frauddetection.producer.FraudAlertProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically polls EasyTrade APIs, runs fraud detection across all accounts,
 * and publishes alerts to the Kafka topic.
 */
@Component
public class FraudDetectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionScheduler.class);

    @Value("${fraud.detection.alert-cooldown-minutes}")
    private int alertCooldownMinutes;

    private final AccountServiceClient accountClient;
    private final BrokerServiceClient brokerClient;
    private final PricingServiceClient pricingClient;
    private final FraudDetectionEngine detectionEngine;
    private final FraudAlertProducer alertProducer;

    /** Tracks last alert time per deduplication key to prevent alert flooding. */
    private final ConcurrentHashMap<String, Instant> alertCooldownCache = new ConcurrentHashMap<>();

    public FraudDetectionScheduler(
            AccountServiceClient accountClient,
            BrokerServiceClient brokerClient,
            PricingServiceClient pricingClient,
            FraudDetectionEngine detectionEngine,
            FraudAlertProducer alertProducer) {
        this.accountClient = accountClient;
        this.brokerClient = brokerClient;
        this.pricingClient = pricingClient;
        this.detectionEngine = detectionEngine;
        this.alertProducer = alertProducer;
    }

    @Scheduled(fixedDelayString = "${fraud.detection.interval-ms}")
    public void runDetectionCycle() {
        log.info("Starting fraud detection cycle");
        long startMs = System.currentTimeMillis();

        List<Account> accounts = accountClient.getAllAccounts();
        if (accounts.isEmpty()) {
            log.warn("No accounts retrieved — skipping detection cycle");
            return;
        }

        Map<Integer, Price> latestPrices = pricingClient.getLatestPrices();
        log.debug("Retrieved {} accounts, {} price quotes", accounts.size(), latestPrices.size());

        int totalAlerts = 0;
        int accountsAnalyzed = 0;

        for (Account account : accounts) {
            if (account.id() == null || Boolean.FALSE.equals(account.accountActive())) {
                continue;
            }

            try {
                totalAlerts += analyzeAccount(account, latestPrices);
                accountsAnalyzed++;
            } catch (Exception e) {
                log.error("Unexpected error analyzing account {}: {}", account.id(), e.getMessage());
            }
        }

        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("Fraud detection cycle complete: {} accounts analyzed, {} alerts generated in {}ms",
                accountsAnalyzed, totalAlerts, elapsedMs);
    }

    private int analyzeAccount(Account account, Map<Integer, Price> latestPrices) {
        int accountId = account.id();

        List<Trade> recentTrades = brokerClient.getTradesForAccount(accountId, 100);
        List<Trade> openLongTrades = brokerClient.getOpenLongTrades(accountId);
        List<InstrumentInfo> instruments = brokerClient.getInstruments(accountId);

        List<FraudAlert> alerts = detectionEngine.analyze(
                account, recentTrades, openLongTrades, latestPrices, instruments);

        int published = 0;
        for (FraudAlert alert : alerts) {
            if (shouldPublish(alert)) {
                alertProducer.publish(alert);
                recordAlert(alert);
                published++;
            }
        }

        return published;
    }

    private boolean shouldPublish(FraudAlert alert) {
        String key = alert.deduplicationKey();
        Instant lastSent = alertCooldownCache.get(key);
        if (lastSent == null) {
            return true;
        }
        long minutesSinceLast = (Instant.now().getEpochSecond() - lastSent.getEpochSecond()) / 60;
        return minutesSinceLast >= alertCooldownMinutes;
    }

    private void recordAlert(FraudAlert alert) {
        alertCooldownCache.put(alert.deduplicationKey(), Instant.now());
        evictExpiredCooldowns();
    }

    private void evictExpiredCooldowns() {
        long cutoffEpoch = Instant.now().getEpochSecond() - ((long) alertCooldownMinutes * 2 * 60);
        alertCooldownCache.entrySet().removeIf(e -> e.getValue().getEpochSecond() < cutoffEpoch);
    }
}
