package cn.itcraft.jdbcmon.monitor;

public final class ExtendedMetricsRecorder implements MetricsRecorder {

    public static final ExtendedMetricsRecorder INSTANCE = new ExtendedMetricsRecorder();

    private ExtendedMetricsRecorder() {}

    @Override
    public void recordSuccess(SqlMetrics metrics, long elapsedNanos, Object result) {
        metrics.addExecutionCount();
        metrics.addTotalTime(elapsedNanos);
        metrics.updateMin(elapsedNanos);
        metrics.updateMax(elapsedNanos);
        if (result instanceof Integer) {
            metrics.addRows((Integer) result);
        } else if (result instanceof int[]) {
            metrics.addRows((int[]) result);
        }
    }

    @Override
    public void recordFailure(SqlMetrics metrics, long elapsedNanos, Throwable t) {
        metrics.addExecutionCount();
        metrics.addFailureCount();
        metrics.addTotalTime(elapsedNanos);
        metrics.updateMin(elapsedNanos);
        metrics.updateMax(elapsedNanos);
    }
}