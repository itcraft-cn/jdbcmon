package cn.itcraft.jdbcmon.monitor;

/**
 * 扩展指标记录器
 * <p>
 * 记录基础指标 + 最小/最大耗时 + 行数统计，性能开销中等（约 5-8%）。
 * <p>
 * <h3>适用场景</h3>
 * <ul>
 *   <li>预发布环境监控</li>
 *   <li>需要详细耗时分析的场景</li>
 * </ul>
 * 
 * @see BasicMetricsRecorder
 * @see FullMetricsRecorder
 */
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