package cn.itcraft.jdbcmon.wrap;

import cn.itcraft.jdbcmon.exception.HugeResultSetException;

import java.sql.SQLException;

/**
 * 超大结果集异常策略
 * 
 * 超过阈值时抛出 HugeResultSetException，中断查询。
 */
final class ThrowExceptionMonitor implements ResultSetMonitor {

    private final String sql;
    private final int threshold;
    private int rowCount = 0;

    ThrowExceptionMonitor(String sql, int threshold) {
        this.sql = sql;
        this.threshold = threshold;
    }

    @Override
    public void onRow() throws SQLException {
        rowCount++;
        if (rowCount == threshold) {
            throw new HugeResultSetException(sql, rowCount, threshold);
        }
    }

    @Override
    public int onClose() {
        return rowCount;
    }
}