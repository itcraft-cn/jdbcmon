package cn.itcraft.jdbcmon.monitor;

public interface MetricsRecorder {

    void recordSuccess(SqlMetrics metrics, long elapsedNanos, Object result);

    void recordFailure(SqlMetrics metrics, long elapsedNanos, Throwable t);
}