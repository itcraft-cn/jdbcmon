package cn.itcraft.jdbcmon.monitor;

import cn.itcraft.jdbcmon.config.WrappedConfig;

import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;

import static cn.itcraft.jdbcmon.consts.JdbcConsts.*;

public final class AdaptiveThreshold {

    private final LongAdder[] timeBuckets;
    private final int windowSizeSeconds;
    private final double percentile;
    private volatile long currentThreshold;
    private volatile long lastUpdateTime;

    public AdaptiveThreshold(WrappedConfig config) {
        this.windowSizeSeconds = config.getAdaptiveWindowSizeSeconds();
        this.percentile = config.getAdaptivePercentile();
        this.timeBuckets = new LongAdder[windowSizeSeconds];
        this.currentThreshold = config.getSlowQueryThresholdMs();

        for (int i = 0; i < timeBuckets.length; i++) {
            timeBuckets[i] = new LongAdder();
        }

        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void record(long elapsedMs) {
        if (elapsedMs < 0) {
            return;
        }

        int bucketIndex = (int) ((System.currentTimeMillis() / 1000) % windowSizeSeconds);
        int bucketValue = normalizeToBucket(elapsedMs);
        timeBuckets[bucketIndex].add(bucketValue);
    }

    public long getThreshold() {
        maybeRecalculate();
        return currentThreshold;
    }

    public void updateThreshold(long newThreshold) {
        if (newThreshold >= MIN_ADAPTIVE_THRESHOLD_MS && newThreshold <= MAX_ADAPTIVE_THRESHOLD_MS) {
            this.currentThreshold = newThreshold;
        }
    }

    private void maybeRecalculate() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < 10000) {
            return;
        }

        synchronized (this) {
            if (now - lastUpdateTime < 10000) {
                return;
            }
            recalculateThreshold();
            lastUpdateTime = now;
        }
    }

    private void recalculateThreshold() {
        long[] buckets = new long[windowSizeSeconds];
        long total = 0;
        int nonZeroCount = 0;

        for (int i = 0; i < windowSizeSeconds; i++) {
            buckets[i] = timeBuckets[i].sum();
            total += buckets[i];
            if (buckets[i] > 0) nonZeroCount++;
        }

        if (nonZeroCount < windowSizeSeconds / 4) {
            return;
        }

        Arrays.sort(buckets);

        int p95Index = (int) (windowSizeSeconds * percentile / 100.0);
        long p95Value = buckets[Math.min(p95Index, buckets.length - 1)];

        if (p95Value > 0) {
            long newThreshold = bucketToMs(p95Value);
            newThreshold = Math.max(MIN_ADAPTIVE_THRESHOLD_MS, Math.min(MAX_ADAPTIVE_THRESHOLD_MS, newThreshold));
            this.currentThreshold = newThreshold;
        }
    }

    private int normalizeToBucket(long elapsedMs) {
        if (elapsedMs < 10) return 1;
        if (elapsedMs < 50) return 2;
        if (elapsedMs < 100) return 3;
        if (elapsedMs < 500) return 4;
        if (elapsedMs < 1000) return 5;
        if (elapsedMs < 5000) return 6;
        return 7;
    }

    private long bucketToMs(long bucketValue) {
        switch ((int) bucketValue) {
            case 1: return 10;
            case 2: return 50;
            case 3: return 100;
            case 4: return 500;
            case 5: return 1000;
            case 6: return 5000;
            case 7: return 10000;
            default: return currentThreshold;
        }
    }

    public void reset() {
        for (LongAdder bucket : timeBuckets) {
            bucket.reset();
        }
        lastUpdateTime = System.currentTimeMillis();
    }
}