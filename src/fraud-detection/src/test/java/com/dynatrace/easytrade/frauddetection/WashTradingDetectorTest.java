package com.dynatrace.easytrade.frauddetection;

import com.dynatrace.easytrade.frauddetection.detector.WashTradingDetector;
import com.dynatrace.easytrade.frauddetection.model.FraudAlert;
import com.dynatrace.easytrade.frauddetection.model.FraudType;
import com.dynatrace.easytrade.frauddetection.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WashTradingDetectorTest {

    private WashTradingDetector detector;

    @BeforeEach
    void setUp() {
        detector = new WashTradingDetector();
        ReflectionTestUtils.setField(detector, "windowMinutes", 60);
    }

    @Test
    void detect_buyAndSellSameInstrumentWithinWindow_returnsAlert() {
        OffsetDateTime now = OffsetDateTime.now();
        Trade buy = new Trade(5, "buy", 10.0, 150.0, now.minusMinutes(30), now.minusMinutes(30),
                true, true, "Instant Buy done.");
        Trade sell = new Trade(5, "sell", 10.0, 152.0, now.minusMinutes(5), now.minusMinutes(5),
                true, true, "Instant Sell done.");

        List<FraudAlert> alerts = detector.detect(42, "testuser", List.of(buy, sell));

        assertFalse(alerts.isEmpty());
        assertEquals(FraudType.WASH_TRADING, alerts.get(0).getFraudType());
        assertEquals(42, alerts.get(0).getAccountId());
        assertEquals(5, alerts.get(0).getInstrumentId());
    }

    @Test
    void detect_buyAndSellOutsideWindow_returnsNoAlert() {
        OffsetDateTime now = OffsetDateTime.now();
        Trade buy = new Trade(5, "buy", 10.0, 150.0, now.minusMinutes(90), now.minusMinutes(90),
                true, true, "Instant Buy done.");
        Trade sell = new Trade(5, "sell", 10.0, 152.0, now.minusMinutes(5), now.minusMinutes(5),
                true, true, "Instant Sell done.");

        List<FraudAlert> alerts = detector.detect(42, "testuser", List.of(buy, sell));

        assertTrue(alerts.isEmpty());
    }

    @Test
    void detect_onlyBuys_returnsNoAlert() {
        OffsetDateTime now = OffsetDateTime.now();
        Trade buy1 = new Trade(5, "buy", 10.0, 150.0, now.minusMinutes(20), now.minusMinutes(20),
                true, true, "Instant Buy done.");
        Trade buy2 = new Trade(5, "buy", 5.0, 151.0, now.minusMinutes(5), now.minusMinutes(5),
                true, true, "Instant Buy done.");

        List<FraudAlert> alerts = detector.detect(42, "testuser", List.of(buy1, buy2));

        assertTrue(alerts.isEmpty());
    }

    @Test
    void detect_differentInstruments_returnsNoAlert() {
        OffsetDateTime now = OffsetDateTime.now();
        Trade buy = new Trade(5, "buy", 10.0, 150.0, now.minusMinutes(20), now.minusMinutes(20),
                true, true, "Instant Buy done.");
        Trade sell = new Trade(7, "sell", 10.0, 200.0, now.minusMinutes(5), now.minusMinutes(5),
                true, true, "Instant Sell done.");

        List<FraudAlert> alerts = detector.detect(42, "testuser", List.of(buy, sell));

        assertTrue(alerts.isEmpty());
    }
}
