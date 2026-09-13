package com.dynatrace.easytrade.frauddetection.client;

import com.dynatrace.easytrade.frauddetection.model.Balance;
import com.dynatrace.easytrade.frauddetection.model.InstrumentInfo;
import com.dynatrace.easytrade.frauddetection.model.Trade;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class BrokerServiceClient {

    private static final Logger log = LoggerFactory.getLogger(BrokerServiceClient.class);

    private final RestClient restClient;

    public BrokerServiceClient(RestClient easyTradeRestClient) {
        this.restClient = easyTradeRestClient;
    }

    /**
     * Fetches the most recent trades for an account.
     * Uses a large page size to capture recent activity for analysis.
     */
    public List<Trade> getTradesForAccount(int accountId, int count) {
        try {
            TradeResultContainer result = restClient.get()
                    .uri("/broker-service/v1/trade/{accountId}?count={count}&page=0", accountId, count)
                    .retrieve()
                    .body(TradeResultContainer.class);
            return result != null && result.results() != null ? result.results() : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("Failed to fetch trades for account {}: {}", accountId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches open long trades for an account (for layering detection).
     */
    public List<Trade> getOpenLongTrades(int accountId) {
        try {
            TradeResultContainer result = restClient.get()
                    .uri("/broker-service/v1/trade/{accountId}?count=200&page=0&onlyOpen=true&onlyLong=true", accountId)
                    .retrieve()
                    .body(TradeResultContainer.class);
            return result != null && result.results() != null ? result.results() : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("Failed to fetch open long trades for account {}: {}", accountId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches the current account balance.
     */
    public Balance getBalance(int accountId) {
        try {
            return restClient.get()
                    .uri("/broker-service/v1/balance/{accountId}", accountId)
                    .retrieve()
                    .body(Balance.class);
        } catch (RestClientException e) {
            log.warn("Failed to fetch balance for account {}: {}", accountId, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches all instruments with optional account ownership data.
     */
    public List<InstrumentInfo> getInstruments(int accountId) {
        try {
            InstrumentsResultContainer result = restClient.get()
                    .uri("/broker-service/v1/instrument?accountId={accountId}", accountId)
                    .retrieve()
                    .body(InstrumentsResultContainer.class);
            return result != null && result.results() != null ? result.results() : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("Failed to fetch instruments for account {}: {}", accountId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TradeResultContainer(List<Trade> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InstrumentsResultContainer(List<InstrumentInfo> results) {}
}
