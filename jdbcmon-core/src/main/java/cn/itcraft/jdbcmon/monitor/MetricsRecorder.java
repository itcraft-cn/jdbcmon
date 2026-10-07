package cn.itcraft.jdbcmon.monitor;

/**
 * 指标记录器策略接口
 * <p>
 * 根据不同的 {@link cn.itcraft.jdbcmon.config.MetricsLevel} 采用不同的记录策略：
 * <ul>
 *   <li>{@link BasicMetricsRecorder}: 仅记录执行次数、错误数、总耗时</li>
 *   <li>{@link ExtendedMetricsRecorder}: 增加最小/最大耗时、平均耗时</li>
 *   <li>{@link FullMetricsRecorder}: 增加直方图统计、百分位耗时</li>
 * </ul>
 * 
 * <h3>性能特点</h3>
 * <ul>
 *   <li>策略模式：消除运行时 switch 分支</li>
 *   <li>单例实现：避免对象创建开销</li>
 *   <li>包级别可见：限制外部直接访问</li>
 * </ul>
 * 
 * @see SqlMonitor#createRecorder(cn.itcraft.jdbcmon.config.MetricsLevel)
 */
public interface MetricsRecorder {

    /**
     * 记录成功执行
     * 
     * @param metrics 目标指标对象
     * @param elapsedNanos 执行耗时（纳秒）
     * @param result 执行结果（行数或 null）
     */
    void recordSuccess(SqlMetrics metrics, long elapsedNanos, Object result);

    /**
     * 记录失败执行
     * 
     * @param metrics 目标指标对象
     * @param elapsedNanos 执行耗时（纳秒）
     * @param t 异常对象
     */
    void recordFailure(SqlMetrics metrics, long elapsedNanos, Throwable t);
}