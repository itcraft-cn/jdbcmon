package cn.itcraft.jdbcmon.monitor;

import cn.itcraft.jdbcmon.config.MetricsLevel;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class SqlMetrics {

    private final String sqlKey;

    private final LongAdder executionCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final LongAdder totalTimeNanos = new LongAdder();

    private final AtomicLong minTimeNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxTimeNanos = new AtomicLong(Long.MIN_VALUE);
    private final LongAdder rowsAffected = new LongAdder();

    private final LongAdder successCount = new LongAdder();
    private final LongAdder[] timeHistogram;

    private static final long[] TIME_BOUNDARIES = {
        1_000_000L,
        10_000_000L,
        50_000_000L,
        100_000_000L,
        500_000_000L,
        1_000_000_000L,
        5_000_000_000L
    };

    public SqlMetrics() {
        this(null);
    }

    public SqlMetrics(String sqlKey) {
        this.sqlKey = sqlKey;
        this.timeHistogram = new LongAdder[TIME_BOUNDARIES.length + 1];
        for (int i = 0; i < timeHistogram.length; i++) {
            timeHistogram[i] = new LongAdder();
        }
    }

    // ========== MetricsRecorder 使用的方法 ==========

    void addExecutionCount() {
        executionCount.increment();
    }

    void addFailureCount() {
        failureCount.increment();
    }

    void addTotalTime(long nanos) {
        totalTimeNanos.add(nanos);
    }

    void addSuccessCount() {
        successCount.increment();
    }

    void updateMin(long elapsedNanos) {
        long current;
        while ((current = minTimeNanos.get()) > elapsedNanos) {
            if (minTimeNanos.compareAndSet(current, elapsedNanos)) {
                break;
            }
        }
    }

    void updateMax(long elapsedNanos) {
        long current;
        while ((current = maxTimeNanos.get()) < elapsedNanos) {
            if (maxTimeNanos.compareAndSet(current, elapsedNanos)) {
                break;
            }
        }
    }

    void addRows(int rows) {
        rowsAffected.add(rows);
    }

    void addRows(int[] rows) {
        for (int count : rows) {
            rowsAffected.add(count);
        }
    }

    void updateHistogramIndex(long elapsedNanos) {
        int index = 0;
        while (index < TIME_BOUNDARIES.length && elapsedNanos > TIME_BOUNDARIES[index]) {
            index++;
        }
        timeHistogram[index].increment();
    }

    // ========== 兼容旧 API（反射模式使用）==========

    public void recordSuccess(long elapsedNanos, Object result, MetricsLevel level) {
        executionCount.increment();
        totalTimeNanos.add(elapsedNanos);

        if (level == MetricsLevel.FULL) {
            successCount.increment();
            updateHistogramIndex(elapsedNanos);
        }

        if (level.ordinal() >= MetricsLevel.EXTENDED.ordinal()) {
            updateMin(elapsedNanos);
            updateMax(elapsedNanos);
            if (result instanceof Integer) {
                rowsAffected.add((Integer) result);
            } else if (result instanceof int[]) {
                for (int count : (int[]) result) {
                    rowsAffected.add(count);
                }
            }
        }
    }

    public void recordFailure(long elapsedNanos, Throwable throwable, MetricsLevel level) {
        executionCount.increment();
        failureCount.increment();
        totalTimeNanos.add(elapsedNanos);

        if (level == MetricsLevel.FULL) {
            updateHistogramIndex(elapsedNanos);
        }

        if (level.ordinal() >= MetricsLevel.EXTENDED.ordinal()) {
            updateMin(elapsedNanos);
            updateMax(elapsedNanos);
        }
    }

    public void recordSuccess(long elapsedNanos, Object result) {
        recordSuccess(elapsedNanos, result, MetricsLevel.FULL);
    }

    public void recordFailure(long elapsedNanos, Throwable throwable) {
        recordFailure(elapsedNanos, throwable, MetricsLevel.FULL);
    }

    // ========== Getter 方法 ==========

    public String getSqlKey() {
        return sqlKey;
    }

    public long getExecutionCount() {
        return executionCount.sum();
    }

    public long getSuccessCount() {
        return successCount.sum();
    }

    public long getFailureCount() {
        return failureCount.sum();
    }

    public long getTotalTimeNanos() {
        return totalTimeNanos.sum();
    }

    public long getMinTimeNanos() {
        long min = minTimeNanos.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxTimeNanos() {
        long max = maxTimeNanos.get();
        return max == Long.MIN_VALUE ? 0 : max;
    }

    public double getAvgTimeNanos() {
        long count = executionCount.sum();
        return count > 0 ? (double) totalTimeNanos.sum() / count : 0;
    }

    public long getRowsAffected() {
        return rowsAffected.sum();
    }

    public long[] getHistogramData() {
        long[] data = new long[timeHistogram.length];
        for (int i = 0; i < timeHistogram.length; i++) {
            data[i] = timeHistogram[i].sum();
        }
        return data;
    }

    public static long[] getTimeBoundaries() {
        return TIME_BOUNDARIES.clone();
    }
}