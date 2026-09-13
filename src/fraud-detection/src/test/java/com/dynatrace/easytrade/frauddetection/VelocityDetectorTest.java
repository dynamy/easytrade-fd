package com.dynatrace.easytrade.frauddetection;

import com.dynatrace.easytrade.frauddetection.detector.VelocityDetector;
import com.dynatrace.easytrade.frauddetection.model.FraudAlert;
import com.dynatrace.easytrade.frauddetection.model.FraudType;
import com.dynatrace.easytrade.frauddetection.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VelocityDetectorTest {

    private VelocityDetector detector;

    @BeforeEach
    void setUp() {
        detector = new VelocityDetector();
        ReflectionTestUtils.setField(detector, "maxTradesPerHour", 5);
    }

    @Test
    void detect_tradesBelowThreshold_returnsNoAlert() {
        List<Trade> trades = buildRecentTrades(4);
        List<FraudAlert> alerts = detector.detect(1, "user", trades);
        assertTrue(alerts.isEmpty());
    }

    @Test
    void detect_tradesAboveThreshold_returnsAlert() {
        List<Trade> trades = buildRecentTrades(10);
        List<FraudAlert> alerts = detector.detect(1, "user", trades);
        assertFalse(alerts.isEmpty());
        assertEquals(FraudType.HIGH_FREQUENCY_TRADING, alerts.get(0).getFraudType());
    }

    @Test
    void detect_oldTradesNotCounted() {
        List<Trade> trades = new ArrayList<>(buildRecentTrades(4));
        // add old trades outside the 1-hour window
        for (int i = 0; i < 10; i++) {
            trades.add(new Trade(i, "buy", 1.0, 100.0,
                    OffsetDateTime.now().minusHours(3), null, true, true, "done"));
        }
        List<FraudAlert> alerts = detector.detect(1, "user", trades);
        assertTrue(alerts.isEmpty());
    }

    private List<Trade> buildRecentTrades(int count) {
        List<Trade> trades = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            trades.add(new Trade(i % 5 + 1, "buy", 1.0, 100.0,
                    OffsetDateTime.now().minusMinutes(i * 3), null, true, true, "done"));
        }
        return trades;
    }
}
