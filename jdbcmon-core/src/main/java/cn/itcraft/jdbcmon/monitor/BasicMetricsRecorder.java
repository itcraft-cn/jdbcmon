package cn.itcraft.jdbcmon.monitor;

public final class BasicMetricsRecorder implements MetricsRecorder {

    public static final BasicMetricsRecorder INSTANCE = new BasicMetricsRecorder();

    private BasicMetricsRecorder() {}

    @Override
    public void recordSuccess(SqlMetrics metrics, long elapsedNanos, Object result) {
        metrics.addExecutionCount();
        metrics.addTotalTime(elapsedNanos);
    }

    @Override
    public void recordFailure(SqlMetrics metrics, long elapsedNanos, Throwable t) {
        metrics.addExecutionCount();
        metrics.addFailureCount();
        metrics.addTotalTime(elapsedNanos);
    }
}