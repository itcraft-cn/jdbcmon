package cn.itcraft.jdbcmon.wrap;

import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.sql.SQLException;

final class NotifyAfterMonitor implements ResultSetMonitor {

    private final SqlMonitor monitor;
    private final String sql;
    private final int threshold;
    private int rowCount = 0;

    NotifyAfterMonitor(SqlMonitor monitor, String sql, int threshold) {
        this.monitor = monitor;
        this.sql = sql;
        this.threshold = threshold;
    }

    @Override
    public void onRow() throws SQLException {
        rowCount++;
    }

    @Override
    public int onClose() {
        monitor.recordResultSetSizeWithNotifyAfter(sql, rowCount, threshold);
        return rowCount;
    }
}