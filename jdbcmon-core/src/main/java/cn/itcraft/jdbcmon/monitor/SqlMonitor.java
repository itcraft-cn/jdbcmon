package cn.itcraft.jdbcmon.monitor;

import cn.itcraft.jdbcmon.config.MetricsLevel;
import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.core.SqlExecutionContext;
import cn.itcraft.jdbcmon.listener.CompositeSqlListener;
import cn.itcraft.jdbcmon.listener.LoggingSqlListener;
import cn.itcraft.jdbcmon.spi.AsyncExecutor;
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
    private final AsyncExecutor asyncExecutor;

    private volatile MetricsLevel currentLevel;

    private final LongAdder totalQueries = new LongAdder();
    private final LongAdder totalUpdates = new LongAdder();
    private final LongAdder totalBatchOps = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();
    private final LongAdder totalSlowQueries = new LongAdder();

    private final AdaptiveThreshold adaptiveThreshold;

    public SqlMonitor(ProxyConfig config) {
        this.config = config;
        this.currentLevel = config.getMetricsLevel();
        this.adaptiveThreshold = config.isUseAdaptiveThreshold() 
            ? new AdaptiveThreshold(config) 
            : null;
        this.asyncExecutor = AsyncExecutor.create(config);

        registerDefaultListeners();
    }

    private void registerDefaultListeners() {
        if (config.isEnableLogging()) {
            listeners.addListener(new LoggingSqlListener());
        }
    }

    // ========== 运行时配置 ==========

    public void setMetricsLevel(MetricsLevel level) {
        this.currentLevel = level;
    }

    public MetricsLevel getMetricsLevel() {
        return currentLevel;
    }

    // ========== Metrics 缓存（供 PreparedStatement 使用）==========

    public SqlMetrics getOrCreateMetrics(String sql) {
        return metricsMap.computeIfAbsent(sql, k -> new SqlMetrics(sql));
    }

    // ========== 快速 API（使用缓存的 Metrics）==========

    public void recordQueryFast(SqlMetrics metrics, long elapsedNanos) {
        totalQueries.increment();
        metrics.recordSuccess(elapsedNanos, null, currentLevel);
        checkSlowQuerySimple(metrics.getSqlKey(), elapsedNanos);
    }

    public void recordUpdateFast(SqlMetrics metrics, long elapsedNanos, int rows) {
        totalUpdates.increment();
        metrics.recordSuccess(elapsedNanos, rows, currentLevel);
        checkSlowQuerySimple(metrics.getSqlKey(), elapsedNanos);
    }

    public void recordBatchFast(SqlMetrics metrics, long elapsedNanos, int[] rows) {
        totalBatchOps.increment();
        metrics.recordSuccess(elapsedNanos, rows, currentLevel);
        checkSlowQuerySimple(metrics.getSqlKey(), elapsedNanos);
    }

    public void recordErrorFast(SqlMetrics metrics, long elapsedNanos, Throwable t) {
        totalErrors.increment();
        metrics.recordFailure(elapsedNanos, t, currentLevel);
    }

    public void recordTransactionFast(SqlMetrics metrics, long elapsedNanos) {
        totalUpdates.increment();
        metrics.recordSuccess(elapsedNanos, null, currentLevel);
    }

    // ========== 简化 API（套壳模式使用，动态 SQL）==========

    public void recordQuery(String sql, long elapsedNanos) {
        if (sql == null || sql.isEmpty()) {
            return;
        }
        totalQueries.increment();
        SqlMetrics metrics = metricsMap.computeIfAbsent(sql, k -> new SqlMetrics(sql));
        metrics.recordSuccess(elapsedNanos, null, currentLevel);
        checkSlowQuerySimple(sql, elapsedNanos);
    }

    public void recordUpdate(String sql, long elapsedNanos, int rows) {
        if (sql == null || sql.isEmpty()) {
            return;
        }
        totalUpdates.increment();
        SqlMetrics metrics = metricsMap.computeIfAbsent(sql, k -> new SqlMetrics(sql));
        metrics.recordSuccess(elapsedNanos, rows, currentLevel);
        checkSlowQuerySimple(sql, elapsedNanos);
    }

    public void recordBatch(String sql, long elapsedNanos, int[] rows) {
        String key = (sql == null || sql.isEmpty()) ? "BATCH_EXECUTION" : sql;
        totalBatchOps.increment();
        SqlMetrics metrics = metricsMap.computeIfAbsent(key, k -> new SqlMetrics(key));
        metrics.recordSuccess(elapsedNanos, rows, currentLevel);
        checkSlowQuerySimple(key, elapsedNanos);
    }

    public void recordTransaction(String operation, long elapsedNanos) {
        totalUpdates.increment();
        String sql = "TRANSACTION_" + operation.toUpperCase();
        SqlMetrics metrics = metricsMap.computeIfAbsent(sql, k -> new SqlMetrics(sql));
        metrics.recordSuccess(elapsedNanos, null, currentLevel);
    }

    public void recordError(String sql, long elapsedNanos, Throwable t) {
        totalErrors.increment();
        String key = (sql == null || sql.isEmpty()) ? "UNKNOWN_SQL" : sql;
        SqlMetrics metrics = metricsMap.computeIfAbsent(key, k -> new SqlMetrics(key));
        metrics.recordFailure(elapsedNanos, t, currentLevel);
    }

    private void checkSlowQuerySimple(String sql, long elapsedNanos) {
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        long threshold = getSlowQueryThreshold();

        if (elapsedMillis > threshold) {
            totalSlowQueries.increment();
            if (config.isLogSlowQueries()) {
                log.warn("[SLOW_SQL] {}ms (threshold: {}ms) - {}", 
                    elapsedMillis, threshold, sql);
            }
        }
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

            if (config.isLogSlowQueries()) {
                log.warn("[SLOW_SQL] {}ms (threshold: {}ms) - {}", 
                    elapsedMillis, threshold, context.getSql());
            }

            listeners.onSlowQuery(context, elapsedMillis);
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