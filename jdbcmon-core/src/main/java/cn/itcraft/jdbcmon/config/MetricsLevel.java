package cn.itcraft.jdbcmon.config;

/**
 * 监控指标级别
 * <p>
 * 不同级别采集不同详细程度的指标：
 * <ul>
 *   <li>BASIC: 基础指标（执行次数、错误数、总耗时），性能开销最低</li>
 *   <li>EXTENDED: 扩展指标（增加平均耗时、最大/最小耗时），性能开销中等</li>
 *   <li>FULL: 完整指标（增加 P95/P99 百分位耗时），性能开销最高</li>
 * </ul>
 * 
 * <h3>性能对比</h3>
 * <ul>
 *   <li>BASIC: 使用 LongAdder 计数，开销约 3-5%</li>
 *   <li>EXTENDED: 增加简单统计，开销约 5-8%</li>
 *   <li>FULL: 增加百分位计算，开销约 10-15%</li>
 * </ul>
 */
public enum MetricsLevel {
    BASIC,
    EXTENDED,
    FULL
}