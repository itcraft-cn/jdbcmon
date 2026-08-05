package cn.itcraft.jdbcmon.wrap;

import java.sql.SQLException;

/**
 * ResultSet 行数监控策略接口
 * <p>
 * 定义在 ResultSet.next() 调用时触发的监控行为，用于实现超大结果集检测。
 * <p>
 * <h3>实现策略</h3>
 * <ul>
 *   <li>{@link #NOOP}：无操作，零开销</li>
 *   <li>ThrowExceptionMonitor：超阈值抛异常</li>
 *   <li>NotifyImmediateMonitor：超阈值立即通知</li>
 *   <li>NotifyAfterMonitor：close 时通知</li>
 * </ul>
 * 
 * @see ResultSetMonitors
 */
interface ResultSetMonitor {

    /**
     * 行数计数（在 next() 时调用）
     * 
     * @throws SQLException 超阈值时抛出（如 ThrowExceptionMonitor）
     */
    void onRow() throws SQLException;

    /**
     * ResultSet 关闭时回调
     * 
     * @return 实际读取的行数
     */
    int onClose();

    ResultSetMonitor NOOP = new ResultSetMonitor() {
        @Override
        public void onRow() {}

        @Override
        public int onClose() { return 0; }
    };
}