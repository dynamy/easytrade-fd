package com.dynatrace.easytrade.frauddetection.producer;

import com.dynatrace.easytrade.frauddetection.model.FraudAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes fraud alerts to the Kafka topic as JSON messages.
 * The message key is the account ID so consumers can partition by account.
 */
@Component
public class FraudAlertProducer {

    private static final Logger log = LoggerFactory.getLogger(FraudAlertProducer.class);

    @Value("${kafka.topic.fraud-alerts}")
    private String topic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FraudAlertProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(FraudAlert alert) {
        String key = String.valueOf(alert.getAccountId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, alert);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish fraud alert [{}] for account {}: {}",
                        alert.getAlertId(), alert.getAccountId(), ex.getMessage());
            } else {
                log.info("Published fraud alert [{}] type={} severity={} account={} partition={} offset={}",
                        alert.getAlertId(),
                        alert.getFraudType(),
                        alert.getSeverity(),
                        alert.getAccountId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
