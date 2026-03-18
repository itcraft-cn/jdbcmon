package cn.itcraft.jdbcmon;

import cn.itcraft.jdbcmon.monitor.SqlMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests for SqlMetrics")
class SqlMetricsTest {

    private SqlMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new SqlMetrics();
    }

    @Test
    @DisplayName("test_recordSuccess_updatesCounters")
    void test_recordSuccess_updatesCounters() {
        metrics.recordSuccess(1_000_000L, null);

        assertEquals(1, metrics.getExecutionCount());
        assertEquals(1, metrics.getSuccessCount());
        assertEquals(0, metrics.getFailureCount());
    }

    @Test
    @DisplayName("test_recordFailure_updatesCounters")
    void test_recordFailure_updatesCounters() {
        metrics.recordFailure(1_000_000L, new RuntimeException("test"));

        assertEquals(1, metrics.getExecutionCount());
        assertEquals(0, metrics.getSuccessCount());
        assertEquals(1, metrics.getFailureCount());
    }

    @Test
    @DisplayName("test_multipleRecords_updatesStatistics")
    void test_multipleRecords_updatesStatistics() {
        metrics.recordSuccess(1_000_000L, null);
        metrics.recordSuccess(2_000_000L, null);
        metrics.recordSuccess(5_000_000L, null);

        assertEquals(3, metrics.getExecutionCount());
        assertEquals(1_000_000L, metrics.getMinTimeNanos());
        assertEquals(5_000_000L, metrics.getMaxTimeNanos());

        double avg = metrics.getAvgTimeNanos();
        assertTrue(avg > 0);
    }

    @Test
    @DisplayName("test_histogram_updatesCorrectly")
    void test_histogram_updatesCorrectly() {
        metrics.recordSuccess(500_000L, null);
        metrics.recordSuccess(5_000_000L, null);
        metrics.recordSuccess(50_000_000L, null);

        long[] histogram = metrics.getHistogramData();
        assertNotNull(histogram);
        assertTrue(histogram.length > 0);

        long total = 0;
        for (long count : histogram) {
            total += count;
        }
        assertEquals(3, total);
    }

    @Test
    @DisplayName("test_rowsAffected_withIntegerResult")
    void test_rowsAffected_withIntegerResult() {
        metrics.recordSuccess(1_000_000L, 5);

        assertEquals(5, metrics.getRowsAffected());
    }

    @Test
    @DisplayName("test_rowsAffected_withIntArrayResult")
    void test_rowsAffected_withIntArrayResult() {
        metrics.recordSuccess(1_000_000L, new int[]{1, 2, 3});

        assertEquals(6, metrics.getRowsAffected());
    }

    @Test
    @DisplayName("test_timeBoundaries_correctOrder")
    void test_timeBoundaries_correctOrder() {
        long[] boundaries = SqlMetrics.getTimeBoundaries();

        for (int i = 1; i < boundaries.length; i++) {
            assertTrue(boundaries[i] > boundaries[i - 1]);
        }
    }
}