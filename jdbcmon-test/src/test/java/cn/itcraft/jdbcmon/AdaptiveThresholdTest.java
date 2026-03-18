package cn.itcraft.jdbcmon;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.monitor.AdaptiveThreshold;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests for AdaptiveThreshold")
class AdaptiveThresholdTest {

    private ProxyConfig config;
    private AdaptiveThreshold threshold;

    @BeforeEach
    void setUp() {
        config = new ProxyConfig.Builder()
            .slowQueryThresholdMs(1000)
            .adaptivePercentile(95.0)
            .adaptiveWindowSize(60)
            .build();

        threshold = new AdaptiveThreshold(config);
    }

    @Test
    @DisplayName("test_getThreshold_returnsInitialValue")
    void test_getThreshold_returnsInitialValue() {
        long thresholdValue = threshold.getThreshold();
        assertEquals(1000L, thresholdValue);
    }

    @Test
    @DisplayName("test_record_updatesThreshold")
    void test_record_updatesThreshold() {
        for (int i = 0; i < 100; i++) {
            threshold.record(50);
        }

        long thresholdValue = threshold.getThreshold();
        assertTrue(thresholdValue > 0);
    }

    @Test
    @DisplayName("test_updateThreshold_respectsBounds")
    void test_updateThreshold_respectsBounds() {
        threshold.updateThreshold(50);
        long min = threshold.getThreshold();
        assertTrue(min >= 100L);

        threshold.updateThreshold(50000);
        long max = threshold.getThreshold();
        assertTrue(max <= 30000L);
    }

    @Test
    @DisplayName("test_reset_clearsData")
    void test_reset_clearsData() {
        for (int i = 0; i < 50; i++) {
            threshold.record(100);
        }

        threshold.reset();

        long thresholdValue = threshold.getThreshold();
        assertTrue(thresholdValue > 0);
    }
}