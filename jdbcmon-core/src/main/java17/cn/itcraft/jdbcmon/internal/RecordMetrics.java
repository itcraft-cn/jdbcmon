package cn.itcraft.jdbcmon.internal;

import cn.itcraft.jdbcmon.monitor.SqlMetrics;

import java.util.concurrent.atomic.LongAdder;

public record RecordMetrics(
    long executionCount,
    long successCount,
    long failureCount,
    long totalTimeNanos,
    long minTimeNanos,
    long maxTimeNanos,
    double avgTimeNanos,
    long rowsAffected,
    long[] histogram
) {

    public static RecordMetrics from(SqlMetrics metrics) {
        return new RecordMetrics(
            metrics.getExecutionCount(),
            metrics.getSuccessCount(),
            metrics.getFailureCount(),
            metrics.getTotalTimeNanos(),
            metrics.getMinTimeNanos(),
            metrics.getMaxTimeNanos(),
            metrics.getAvgTimeNanos(),
            metrics.getRowsAffected(),
            metrics.getHistogramData()
        );
    }

    public double getAvgTimeMillis() {
        return avgTimeNanos / 1_000_000.0;
    }

    public long getMinTimeMillis() {
        return minTimeNanos / 1_000_000;
    }

    public long getMaxTimeMillis() {
        return maxTimeNanos / 1_000_000;
    }

    public double getTotalTimeMillis() {
        return totalTimeNanos / 1_000_000.0;
    }

    public double getSuccessRate() {
        return executionCount > 0 ? (double) successCount / executionCount : 0;
    }

    public double getFailureRate() {
        return executionCount > 0 ? (double) failureCount / executionCount : 0;
    }
}