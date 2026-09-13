package com.dynatrace.easytrade.frauddetection.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    @Value("${kafka.topic.fraud-alerts}")
    private String fraudAlertsTopic;

    /**
     * Auto-creates the fraud alerts topic on startup if it does not yet exist.
     * Partitioned by 3 so consumers can parallelize by account shard.
     */
    @Bean
    public NewTopic fraudAlertsTopic() {
        return TopicBuilder.name(fraudAlertsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
