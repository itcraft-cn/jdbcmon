package cn.itcraft.jdbcmon.monitor;

import java.util.concurrent.atomic.LongAdder;

public final class SqlMetrics {

    private final LongAdder executionCount = new LongAdder();
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final LongAdder totalTimeNanos = new LongAdder();

    private volatile long minTimeNanos = Long.MAX_VALUE;
    private volatile long maxTimeNanos = Long.MIN_VALUE;

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

    private final LongAdder rowsAffected = new LongAdder();

    public SqlMetrics() {
        timeHistogram = new LongAdder[TIME_BOUNDARIES.length + 1];
        for (int i = 0; i < timeHistogram.length; i++) {
            timeHistogram[i] = new LongAdder();
        }
    }

    public void recordSuccess(long elapsedNanos, Object result) {
        executionCount.increment();
        successCount.increment();
        totalTimeNanos.add(elapsedNanos);

        updateMinTime(elapsedNanos);
        updateMaxTime(elapsedNanos);
        updateHistogram(elapsedNanos);

        if (result instanceof Integer) {
            rowsAffected.add((Integer) result);
        } else if (result instanceof int[]) {
            for (int count : (int[]) result) {
                rowsAffected.add(count);
            }
        }
    }

    public void recordFailure(long elapsedNanos, Throwable throwable) {
        executionCount.increment();
        failureCount.increment();
        totalTimeNanos.add(elapsedNanos);

        updateMinTime(elapsedNanos);
        updateMaxTime(elapsedNanos);
        updateHistogram(elapsedNanos);
    }

    private void updateMinTime(long elapsedNanos) {
        long current = minTimeNanos;
        while (elapsedNanos < current) {
            if (compareAndSetMin(current, elapsedNanos)) {
                break;
            }
            current = minTimeNanos;
        }
    }

    private void updateMaxTime(long elapsedNanos) {
        long current = maxTimeNanos;
        while (elapsedNanos > current) {
            if (compareAndSetMax(current, elapsedNanos)) {
                break;
            }
            current = maxTimeNanos;
        }
    }

    private synchronized boolean compareAndSetMin(long expect, long update) {
        if (minTimeNanos == expect) {
            minTimeNanos = update;
            return true;
        }
        return false;
    }

    private synchronized boolean compareAndSetMax(long expect, long update) {
        if (maxTimeNanos == expect) {
            maxTimeNanos = update;
            return true;
        }
        return false;
    }

    private void updateHistogram(long elapsedNanos) {
        int index = 0;
        while (index < TIME_BOUNDARIES.length && elapsedNanos > TIME_BOUNDARIES[index]) {
            index++;
        }
        timeHistogram[index].increment();
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
        long min = minTimeNanos;
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxTimeNanos() {
        long max = maxTimeNanos;
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