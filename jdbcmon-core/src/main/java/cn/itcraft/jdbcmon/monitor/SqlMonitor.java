package cn.itcraft.jdbcmon.monitor;

import cn.itcraft.jdbcmon.config.MetricsLevel;
import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.core.SqlExecutionContext;
import cn.itcraft.jdbcmon.internal.PlatformThreadExecutor;
import cn.itcraft.jdbcmon.listener.CompositeSqlListener;
import cn.itcraft.jdbcmon.listener.LoggingSqlListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class SqlMonitor {

    private static final Logger log = LoggerFactory.getLogger(SqlMonitor.class);

    private final ProxyConfig config;
    private final CompositeSqlListener listeners = new CompositeSqlListener();
    private final Map<String, SqlMetrics> metricsMap = new ConcurrentHashMap<>();
    private final PlatformThreadExecutor asyncExecutor;

    private volatile MetricsLevel currentLevel;
    private volatile MetricsRecorder recorder;
    private volatile long slowQueryThresholdNanos;

    private final LongAdder totalQueries = new LongAdder();
    private final LongAdder totalUpdates = new LongAdder();
    private final LongAdder totalBatchOps = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();
    private final LongAdder totalSlowQueries = new LongAdder();

    private final AdaptiveThreshold adaptiveThreshold;
    private final boolean logSlowQueries;

    public SqlMonitor(ProxyConfig config) {
        this.config = config;
        this.currentLevel = config.getMetricsLevel();
        this.recorder = createRecorder(currentLevel);
        this.slowQueryThresholdNanos = TimeUnit.MILLISECONDS.toNanos(config.getSlowQueryThresholdMs());
        this.logSlowQueries = config.isLogSlowQueries();
        this.adaptiveThreshold = config.isUseAdaptiveThreshold() 
            ? new AdaptiveThreshold(config) 
            : null;
        this.asyncExecutor = new PlatformThreadExecutor(config);

        registerDefaultListeners();
    }

    private void registerDefaultListeners() {
        if (config.isEnableLogging()) {
            listeners.addListener(new LoggingSqlListener());
        }
    }

    private MetricsRecorder createRecorder(MetricsLevel level) {
        switch (level) {
            case BASIC:
                return BasicMetricsRecorder.INSTANCE;
            case EXTENDED:
                return ExtendedMetricsRecorder.INSTANCE;
            case FULL:
            default:
                return FullMetricsRecorder.INSTANCE;
        }
    }

    // ========== 运行时配置 ==========

    public void setMetricsLevel(MetricsLevel level) {
        this.currentLevel = level;
        this.recorder = createRecorder(level);
    }

    public MetricsLevel getMetricsLevel() {
        return currentLevel;
    }

    public void setSlowQueryThresholdMs(long thresholdMs) {
        this.slowQueryThresholdNanos = TimeUnit.MILLISECONDS.toNanos(thresholdMs);
    }

    // ========== Metrics 缓存（供 PreparedStatement 使用）==========

    public SqlMetrics getOrCreateMetrics(String sql) {
        return metricsMap.computeIfAbsent(sql, k -> new SqlMetrics(sql));
    }

    // ========== 快速 API（使用缓存的 Metrics）==========

    public void recordQueryFast(SqlMetrics metrics, long elapsedNanos) {
        totalQueries.increment();
        recorder.recordSuccess(metrics, elapsedNanos, null);
        checkSlowQueryFast(metrics.getSqlKey(), elapsedNanos);
    }

    public void recordUpdateFast(SqlMetrics metrics, long elapsedNanos, int rows) {
        totalUpdates.increment();
        recorder.recordSuccess(metrics, elapsedNanos, rows);
        checkSlowQueryFast(metrics.getSqlKey(), elapsedNanos);
    }

    public void recordBatchFast(SqlMetrics metrics, long elapsedNanos, int[] rows) {
        totalBatchOps.increment();
        recorder.recordSuccess(metrics, elapsedNanos, rows);
        checkSlowQueryFast(metrics.getSqlKey(), elapsedNanos);
    }

    public void recordErrorFast(SqlMetrics metrics, long elapsedNanos, Throwable t) {
        totalErrors.increment();
        recorder.recordFailure(metrics, elapsedNanos, t);
    }

    public void recordTransactionFast(SqlMetrics metrics, long elapsedNanos) {
        totalUpdates.increment();
        recorder.recordSuccess(metrics, elapsedNanos, null);
    }

    private void checkSlowQueryFast(String sql, long elapsedNanos) {
        if (elapsedNanos > slowQueryThresholdNanos) {
            totalSlowQueries.increment();
            if (logSlowQueries) {
                long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
                long thresholdMs = TimeUnit.NANOSECONDS.toMillis(slowQueryThresholdNanos);
                log.warn("[SLOW_SQL] {}ms (threshold: {}ms) - {}", elapsedMillis, thresholdMs, sql);
            }
        }
    }

    // ========== 简化 API（套壳模式使用，动态 SQL）==========

    public void recordQuery(String sql, long elapsedNanos) {
        if (sql == null || sql.isEmpty()) {
            return;
        }
        totalQueries.increment();
        SqlMetrics metrics = metricsMap.computeIfAbsent(sql, k -> new SqlMetrics(sql));
        recorder.recordSuccess(metrics, elapsedNanos, null);
        checkSlowQueryFast(sql, elapsedNanos);
    }

    public void recordUpdate(String sql, long elapsedNanos, int rows) {
        if (sql == null || sql.isEmpty()) {
            return;
        }
        totalUpdates.increment();
        SqlMetrics metrics = metricsMap.computeIfAbsent(sql, k -> new SqlMetrics(sql));
        recorder.recordSuccess(metrics, elapsedNanos, rows);
        checkSlowQueryFast(sql, elapsedNanos);
    }

    public void recordBatch(String sql, long elapsedNanos, int[] rows) {
        String key = (sql == null || sql.isEmpty()) ? "BATCH_EXECUTION" : sql;
        totalBatchOps.increment();
        SqlMetrics metrics = metricsMap.computeIfAbsent(key, k -> new SqlMetrics(key));
        recorder.recordSuccess(metrics, elapsedNanos, rows);
        checkSlowQueryFast(key, elapsedNanos);
    }

    public void recordTransaction(String operation, long elapsedNanos) {
        totalUpdates.increment();
        String sql = "TRANSACTION_" + operation.toUpperCase();
        SqlMetrics metrics = metricsMap.computeIfAbsent(sql, k -> new SqlMetrics(sql));
        recorder.recordSuccess(metrics, elapsedNanos, null);
    }

    public void recordError(String sql, long elapsedNanos, Throwable t) {
        totalErrors.increment();
        String key = (sql == null || sql.isEmpty()) ? "UNKNOWN_SQL" : sql;
        SqlMetrics metrics = metricsMap.computeIfAbsent(key, k -> new SqlMetrics(key));
        recorder.recordFailure(metrics, elapsedNanos, t);
    }

    // ========== 原有 API（反射模式使用）==========

    public void recordSuccess(SqlExecutionContext context, long elapsedNanos, Object result) {
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        String sql = context.getSql();

        updateCounters(context.getMethodName());

        if (sql != null && !sql.isEmpty()) {
            SqlMetrics metrics = metricsMap.computeIfAbsent(sql, k -> new SqlMetrics());
            metrics.recordSuccess(elapsedNanos, result);
        }

        if (adaptiveThreshold != null) {
            adaptiveThreshold.record(elapsedMillis);
        }

        checkSlowQuery(context, elapsedMillis);

        notifyListenersAsync(context, elapsedNanos, result);
    }

    public void recordFailure(SqlExecutionContext context, long elapsedNanos, Throwable throwable) {
        totalErrors.increment();

        String sql = context.getSql();
        if (sql != null && !sql.isEmpty()) {
            SqlMetrics metrics = metricsMap.computeIfAbsent(sql, k -> new SqlMetrics());
            metrics.recordFailure(elapsedNanos, throwable);
        }

        notifyFailureListenersAsync(context, elapsedNanos, throwable);
    }

    private void updateCounters(String methodName) {
        if (methodName == null) {
            return;
        }

        if (methodName.contains("Query") || methodName.equals("execute")) {
            totalQueries.increment();
        } else if (methodName.contains("Update")) {
            totalUpdates.increment();
        } else if (methodName.contains("Batch")) {
            totalBatchOps.increment();
        }
    }

    private void checkSlowQuery(SqlExecutionContext context, long elapsedMillis) {
        long threshold = getSlowQueryThreshold();

        if (elapsedMillis > threshold) {
            totalSlowQueries.increment();

            if (config.isCollectStackTrace()) {
                context.setStackTrace(Thread.currentThread().getStackTrace());
            }

            if (logSlowQueries) {
                log.warn("[SLOW_SQL] {}ms (threshold: {}ms) - {}", 
                    elapsedMillis, threshold, context.getSql());
            }

            notifySlowQueryAsync(context, elapsedMillis);
        }
    }

    private long getSlowQueryThreshold() {
        if (adaptiveThreshold != null) {
            return adaptiveThreshold.getThreshold();
        }
        return config.getSlowQueryThresholdMs();
    }

    private void notifyListenersAsync(SqlExecutionContext context, long elapsedNanos, Object result) {
        if (listeners.getListeners().isEmpty()) {
            return;
        }

        asyncExecutor.submit(() -> {
            listeners.onSuccess(context, elapsedNanos, result);
        });
    }

    private void notifyFailureListenersAsync(SqlExecutionContext context, long elapsedNanos, Throwable throwable) {
        if (listeners.getListeners().isEmpty()) {
            return;
        }

        asyncExecutor.submit(() -> {
            listeners.onFailure(context, elapsedNanos, throwable);
        });
    }

    private void notifySlowQueryAsync(SqlExecutionContext context, long elapsedMillis) {
        if (listeners.getListeners().isEmpty()) {
            return;
        }

        asyncExecutor.submit(() -> {
            listeners.onSlowQuery(context, elapsedMillis);
        });
    }

    public SqlStatistics getStatistics() {
        SqlStatistics stats = new SqlStatistics();
        stats.setTotalQueries(totalQueries.sum());
        stats.setTotalUpdates(totalUpdates.sum());
        stats.setTotalBatchOps(totalBatchOps.sum());
        stats.setTotalErrors(totalErrors.sum());
        stats.setTotalSlowQueries(totalSlowQueries.sum());

        List<Map.Entry<String, SqlMetrics>> entries = new ArrayList<>(metricsMap.entrySet());
        entries.sort((e1, e2) -> Long.compare(e2.getValue().getMaxTimeNanos(), e1.getValue().getMaxTimeNanos()));

        int limit = Math.min(config.getTopSlowQueryLimit(), entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, SqlMetrics> entry = entries.get(i);
            stats.addSlowQuery(entry.getKey(), entry.getValue());
        }

        stats.setMetricsMap(new ConcurrentHashMap<>(metricsMap));
        stats.setCurrentSlowQueryThreshold(getSlowQueryThreshold());

        return stats;
    }

    public void addListener(cn.itcraft.jdbcmon.listener.SqlExecutionListener listener) {
        listeners.addListener(listener);
    }

    public void removeListener(cn.itcraft.jdbcmon.listener.SqlExecutionListener listener) {
        listeners.removeListener(listener);
    }

    public Map<String, SqlMetrics> getMetricsMap() {
        return metricsMap;
    }

    public void shutdown() {
        asyncExecutor.shutdown();
    }

    public ProxyConfig getConfig() {
        return config;
    }
}