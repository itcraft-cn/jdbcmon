package cn.itcraft.jdbcmon.spring.properties;

import cn.itcraft.jdbcmon.config.WrappedConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jdbcmon")
public class JdbcMonProperties {

    private boolean enabled = true;

    private long slowQueryThresholdMs = 1000L;

    private boolean logSlowQueries = true;

    private boolean collectStackTrace = false;

    private boolean useAdaptiveThreshold = true;

    private double adaptivePercentile = 95.0;

    private int adaptiveWindowSizeSeconds = 60;

    private ThreadPool threadPool = new ThreadPool();

    private Monitoring monitoring = new Monitoring();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getSlowQueryThresholdMs() {
        return slowQueryThresholdMs;
    }

    public void setSlowQueryThresholdMs(long slowQueryThresholdMs) {
        this.slowQueryThresholdMs = slowQueryThresholdMs;
    }

    public boolean isLogSlowQueries() {
        return logSlowQueries;
    }

    public void setLogSlowQueries(boolean logSlowQueries) {
        this.logSlowQueries = logSlowQueries;
    }

    public boolean isCollectStackTrace() {
        return collectStackTrace;
    }

    public void setCollectStackTrace(boolean collectStackTrace) {
        this.collectStackTrace = collectStackTrace;
    }

    public boolean isUseAdaptiveThreshold() {
        return useAdaptiveThreshold;
    }

    public void setUseAdaptiveThreshold(boolean useAdaptiveThreshold) {
        this.useAdaptiveThreshold = useAdaptiveThreshold;
    }

    public double getAdaptivePercentile() {
        return adaptivePercentile;
    }

    public void setAdaptivePercentile(double adaptivePercentile) {
        this.adaptivePercentile = adaptivePercentile;
    }

    public int getAdaptiveWindowSizeSeconds() {
        return adaptiveWindowSizeSeconds;
    }

    public void setAdaptiveWindowSizeSeconds(int adaptiveWindowSizeSeconds) {
        this.adaptiveWindowSizeSeconds = adaptiveWindowSizeSeconds;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    public Monitoring getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(Monitoring monitoring) {
        this.monitoring = monitoring;
    }

    public WrappedConfig toConfig() {
        return new WrappedConfig.Builder()
            .enableMonitoring(enabled)
            .slowQueryThresholdMs(slowQueryThresholdMs)
            .logSlowQueries(logSlowQueries)
            .collectStackTrace(collectStackTrace)
            .useAdaptiveThreshold(useAdaptiveThreshold)
            .adaptivePercentile(adaptivePercentile)
            .adaptiveWindowSize(adaptiveWindowSizeSeconds)
            .threadPool(threadPool.getCoreSize(), threadPool.getMaxSize(), threadPool.getQueueCapacity())
            .monitorConnections(monitoring.isConnections())
            .monitorTransactions(monitoring.isTransactions())
            .monitorBatchOperations(monitoring.isBatchOperations())
            .build();
    }

    public static class ThreadPool {
        private int coreSize = 2;
        private int maxSize = 4;
        private int queueCapacity = 1000;

        public int getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(int coreSize) {
            this.coreSize = coreSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class Monitoring {
        private boolean connections = true;
        private boolean transactions = true;
        private boolean batchOperations = true;

        public boolean isConnections() {
            return connections;
        }

        public void setConnections(boolean connections) {
            this.connections = connections;
        }

        public boolean isTransactions() {
            return transactions;
        }

        public void setTransactions(boolean transactions) {
            this.transactions = transactions;
        }

        public boolean isBatchOperations() {
            return batchOperations;
        }

        public void setBatchOperations(boolean batchOperations) {
            this.batchOperations = batchOperations;
        }
    }
}