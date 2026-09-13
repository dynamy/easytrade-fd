package com.dynatrace.easytrade.frauddetection.client;

import com.dynatrace.easytrade.frauddetection.model.Account;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class AccountServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClient.class);

    private final RestClient restClient;

    public AccountServiceClient(RestClient easyTradeRestClient) {
        this.restClient = easyTradeRestClient;
    }

    /**
     * Fetches all accounts from the manager service.
     * Returns an empty list if the service is unavailable.
     */
    public List<Account> getAllAccounts() {
        try {
            Account[] accounts = restClient.get()
                    .uri("/manager/api/Accounts/")
                    .retrieve()
                    .body(Account[].class);
            if (accounts == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(accounts);
        } catch (RestClientException e) {
            log.warn("Failed to fetch accounts from manager: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches a single account by ID from the account service.
     */
    public Account getAccount(int accountId) {
        try {
            return restClient.get()
                    .uri("/accountservice/accounts/{id}", accountId)
                    .retrieve()
                    .body(Account.class);
        } catch (RestClientException e) {
            log.warn("Failed to fetch account {}: {}", accountId, e.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccountsContainer(List<Account> accounts) {}
}
