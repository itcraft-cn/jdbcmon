package cn.itcraft.jdbcmon.wrap;

import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.sql.SQLException;

final class NotifyImmediateMonitor implements ResultSetMonitor {

    private final SqlMonitor monitor;
    private final String sql;
    private final int threshold;
    private int rowCount = 0;
    private boolean notified = false;

    NotifyImmediateMonitor(SqlMonitor monitor, String sql, int threshold) {
        this.monitor = monitor;
        this.sql = sql;
        this.threshold = threshold;
    }

    @Override
    public void onRow() throws SQLException {
        rowCount++;
        if (!notified && rowCount >= threshold) {
            notified = true;
            monitor.notifyHugeResultSet(sql, rowCount);
        }
    }

    @Override
    public int onClose() {
        monitor.recordResultSetSize(sql, rowCount);
        return rowCount;
    }
}