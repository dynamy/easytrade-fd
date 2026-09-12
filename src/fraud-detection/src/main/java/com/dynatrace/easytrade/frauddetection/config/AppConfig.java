package com.dynatrace.easytrade.frauddetection.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Value("${easytrade.base-url}")
    private String easyTradeBaseUrl;

    /**
     * Spring Boot auto-registers any Jackson Module beans into the shared ObjectMapper.
     * This ensures OffsetDateTime fields in API responses deserialize correctly.
     */
    @Bean
    public JavaTimeModule javaTimeModule() {
        return new JavaTimeModule();
    }

    @Bean
    public RestClient easyTradeRestClient() {
        return RestClient.builder()
                .baseUrl(easyTradeBaseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
