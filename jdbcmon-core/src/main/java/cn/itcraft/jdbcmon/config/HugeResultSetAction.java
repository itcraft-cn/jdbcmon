package cn.itcraft.jdbcmon.config;

/**
 * 超大结果集处理策略
 * <p>
 * 当 ResultSet 行数超过 {@link WrappedConfig#hugeResultSetThreshold} 时触发：
 * <ul>
 *   <li>THROW_EXCEPTION: 立即抛出 {@link cn.itcraft.jdbcmon.exception.HugeResultSetException}，终止查询</li>
 *   <li>NOTIFY_IMMEDIATE: 立即发送告警事件，继续执行</li>
 *   <li>NOTIFY_AFTER: 等待结果集完全读取后发送告警事件</li>
 * </ul>
 * 
 * @see cn.itcraft.jdbcmon.exception.HugeResultSetException
 */
public enum HugeResultSetAction {
    THROW_EXCEPTION,
    NOTIFY_IMMEDIATE,
    NOTIFY_AFTER
}