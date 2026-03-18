package cn.itcraft.jdbcmon.config;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static cn.itcraft.jdbcmon.consts.JdbcConsts.*;

public final class ProxyConfig {

    private ProxyMode proxyMode = ProxyMode.WRAPPER;
    
    private boolean enableMonitoring = true;
    private long slowQueryThresholdMs = DEFAULT_SLOW_QUERY_THRESHOLD_MS;
    private boolean logSlowQueries = true;
    private boolean collectStackTrace = false;

    private Set<String> excludedTables = new HashSet<>();
    private Set<String> excludedSchemas = new HashSet<>();
    private Pattern sqlPatternFilter = null;

    private int corePoolSize = DEFAULT_CORE_POOL_SIZE;
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    private int queueCapacity = DEFAULT_QUEUE_CAPACITY;

    private boolean enableLogging = true;
    private boolean logParameters = false;

    private boolean enableMetrics = true;
    private int metricsFlushIntervalSeconds = 60;
    private int topSlowQueryLimit = 10;

    private boolean monitorConnections = true;
    private boolean monitorTransactions = true;
    private boolean monitorBatchOperations = true;

    private boolean useAdaptiveThreshold = true;
    private double adaptivePercentile = ADAPTIVE_PERCENTILE;
    private int adaptiveWindowSizeSeconds = ADAPTIVE_WINDOW_SIZE_SECONDS;

    private ProxyConfig() {
    }

    public ProxyMode getProxyMode() {
        return proxyMode;
    }

    public boolean isEnableMonitoring() {
        return enableMonitoring;
    }

    public long getSlowQueryThresholdMs() {
        return slowQueryThresholdMs;
    }

    public boolean isLogSlowQueries() {
        return logSlowQueries;
    }

    public boolean isCollectStackTrace() {
        return collectStackTrace;
    }

    public Set<String> getExcludedTables() {
        return excludedTables;
    }

    public Set<String> getExcludedSchemas() {
        return excludedSchemas;
    }

    public Pattern getSqlPatternFilter() {
        return sqlPatternFilter;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public boolean isLogParameters() {
        return logParameters;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public int getMetricsFlushIntervalSeconds() {
        return metricsFlushIntervalSeconds;
    }

    public int getTopSlowQueryLimit() {
        return topSlowQueryLimit;
    }

    public boolean isMonitorConnections() {
        return monitorConnections;
    }

    public boolean isMonitorTransactions() {
        return monitorTransactions;
    }

    public boolean isMonitorBatchOperations() {
        return monitorBatchOperations;
    }

    public boolean isUseAdaptiveThreshold() {
        return useAdaptiveThreshold;
    }

    public double getAdaptivePercentile() {
        return adaptivePercentile;
    }

    public int getAdaptiveWindowSizeSeconds() {
        return adaptiveWindowSizeSeconds;
    }

    public boolean shouldFilter(String sql) {
        if (sql == null || sql.isEmpty()) {
            return true;
        }

        String lowerSql = sql.toLowerCase();
        for (String table : excludedTables) {
            if (lowerSql.contains(table.toLowerCase())) {
                return true;
            }
        }

        for (String schema : excludedSchemas) {
            if (lowerSql.contains(schema.toLowerCase() + ".")) {
                return true;
            }
        }

        if (sqlPatternFilter != null && sqlPatternFilter.matcher(sql).find()) {
            return true;
        }

        return false;
    }

    public static class Builder {
        private final ProxyConfig config = new ProxyConfig();

        public Builder proxyMode(ProxyMode mode) {
            config.proxyMode = mode;
            return this;
        }

        public Builder enableMonitoring(boolean enable) {
            config.enableMonitoring = enable;
            return this;
        }

        public Builder slowQueryThresholdMs(long threshold) {
            config.slowQueryThresholdMs = threshold;
            return this;
        }

        public Builder logSlowQueries(boolean enable) {
            config.logSlowQueries = enable;
            return this;
        }

        public Builder collectStackTrace(boolean enable) {
            config.collectStackTrace = enable;
            return this;
        }

        public Builder addExcludedTable(String table) {
            config.excludedTables.add(table.toLowerCase());
            return this;
        }

        public Builder addExcludedSchema(String schema) {
            config.excludedSchemas.add(schema.toLowerCase());
            return this;
        }

        public Builder sqlPatternFilter(String regex) {
            if (regex != null && !regex.isEmpty()) {
                config.sqlPatternFilter = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            }
            return this;
        }

        public Builder threadPool(int core, int max, int queue) {
            config.corePoolSize = core;
            config.maxPoolSize = max;
            config.queueCapacity = queue;
            return this;
        }

        public Builder enableLogging(boolean enable) {
            config.enableLogging = enable;
            return this;
        }

        public Builder logParameters(boolean enable) {
            config.logParameters = enable;
            return this;
        }

        public Builder enableMetrics(boolean enable) {
            config.enableMetrics = enable;
            return this;
        }

        public Builder metricsFlushInterval(int seconds) {
            config.metricsFlushIntervalSeconds = seconds;
            return this;
        }

        public Builder topSlowQueryLimit(int limit) {
            config.topSlowQueryLimit = limit;
            return this;
        }

        public Builder monitorConnections(boolean enable) {
            config.monitorConnections = enable;
            return this;
        }

        public Builder monitorTransactions(boolean enable) {
            config.monitorTransactions = enable;
            return this;
        }

        public Builder monitorBatchOperations(boolean enable) {
            config.monitorBatchOperations = enable;
            return this;
        }

        public Builder useAdaptiveThreshold(boolean enable) {
            config.useAdaptiveThreshold = enable;
            return this;
        }

        public Builder adaptivePercentile(double percentile) {
            config.adaptivePercentile = percentile;
            return this;
        }

        public Builder adaptiveWindowSize(int seconds) {
            config.adaptiveWindowSizeSeconds = seconds;
            return this;
        }

        public ProxyConfig build() {
            return config;
        }
    }
}