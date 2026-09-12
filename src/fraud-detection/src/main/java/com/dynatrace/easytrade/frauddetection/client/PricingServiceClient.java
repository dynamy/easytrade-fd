package com.dynatrace.easytrade.frauddetection.client;

import com.dynatrace.easytrade.frauddetection.model.Price;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PricingServiceClient {

    private static final Logger log = LoggerFactory.getLogger(PricingServiceClient.class);

    private final RestClient restClient;

    public PricingServiceClient(RestClient easyTradeRestClient) {
        this.restClient = easyTradeRestClient;
    }

    /**
     * Fetches the latest price for every instrument, indexed by instrumentId.
     */
    public Map<Integer, Price> getLatestPrices() {
        try {
            PricesResultContainer result = restClient.get()
                    .uri("/pricing-service/v1/prices/latest")
                    .retrieve()
                    .body(PricesResultContainer.class);
            if (result == null || result.results() == null) {
                return Collections.emptyMap();
            }
            return result.results().stream()
                    .filter(p -> p.instrumentId() != null)
                    .collect(Collectors.toMap(Price::instrumentId, Function.identity(), (a, b) -> b));
        } catch (RestClientException e) {
            log.warn("Failed to fetch latest prices: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Fetches recent price history for a specific instrument.
     */
    public List<Price> getPriceHistory(int instrumentId, int records) {
        try {
            PricesResultContainer result = restClient.get()
                    .uri("/pricing-service/v1/prices/instrument/{instrumentId}?records={records}",
                            instrumentId, records)
                    .retrieve()
                    .body(PricesResultContainer.class);
            return result != null && result.results() != null ? result.results() : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("Failed to fetch price history for instrument {}: {}", instrumentId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PricesResultContainer(List<Price> results) {}
}
