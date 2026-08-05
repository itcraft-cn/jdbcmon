package cn.itcraft.jdbcmon.monitor;

/**
 * 基础指标记录器
 * <p>
 * 仅记录核心指标（执行次数、错误数、总耗时），性能开销最低（约 3-5%）。
 * <p>
 * <h3>适用场景</h3>
 * <ul>
 *   <li>生产环境低负载监控</li>
 *   <li>高并发场景下最小化性能影响</li>
 * </ul>
 * 
 * @see ExtendedMetricsRecorder
 * @see FullMetricsRecorder
 */
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