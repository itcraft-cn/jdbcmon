package cn.itcraft.jdbcmon.monitor;

/**
 * 完整指标记录器
 * <p>
 * 记录全部指标（基础指标 + 扩展指标 + 直方图统计 + 百分位耗时），性能开销最高（约 10-15%）。
 * <p>
 * <h3>适用场景</h3>
 * <ul>
 *   <li>性能测试和基准测试</li>
 *   <li>需要百分位耗时分析的场景（P95/P99）</li>
 *   <li>开发环境详细监控</li>
 * </ul>
 * 
 * @see BasicMetricsRecorder
 * @see ExtendedMetricsRecorder
 */
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