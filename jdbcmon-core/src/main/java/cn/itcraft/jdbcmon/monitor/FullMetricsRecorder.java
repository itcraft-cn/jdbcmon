package cn.itcraft.jdbcmon.monitor;

public final class FullMetricsRecorder implements MetricsRecorder {

    public static final FullMetricsRecorder INSTANCE = new FullMetricsRecorder();

    private FullMetricsRecorder() {}

    @Override
    public void recordSuccess(SqlMetrics metrics, long elapsedNanos, Object result) {
        metrics.addExecutionCount();
        metrics.addTotalTime(elapsedNanos);
        metrics.addSuccessCount();
        metrics.updateHistogramIndex(elapsedNanos);
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
        metrics.updateHistogramIndex(elapsedNanos);
        metrics.updateMin(elapsedNanos);
        metrics.updateMax(elapsedNanos);
    }
}