package cn.itcraft.jdbcmon.wrap;

import cn.itcraft.jdbcmon.config.HugeResultSetAction;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

/**
 * ResultSetMonitor 策略工厂
 * 
 * 根据 HugeResultSetAction 创建对应的监控策略实例。
 */
final class ResultSetMonitors {

    private ResultSetMonitors() {}

    static ResultSetMonitor create(SqlMonitor monitor, String sql,
            int threshold, HugeResultSetAction action) {
        if (threshold <= 0 || action == null) {
            return ResultSetMonitor.NOOP;
        }
        switch (action) {
            case THROW_EXCEPTION:
                return new ThrowExceptionMonitor(sql, threshold);
            case NOTIFY_IMMEDIATE:
                return new NotifyImmediateMonitor(monitor, sql, threshold);
            case NOTIFY_AFTER:
                return new NotifyAfterMonitor(monitor, sql, threshold);
            default:
                return ResultSetMonitor.NOOP;
        }
    }
}