package cn.itcraft.jdbcmon.exception;

import java.sql.SQLException;

/**
 * 超大结果集异常
 * <p>
 * 当 ResultSet 行数超过阈值时抛出（需配置 HugeResultSetAction.THROW_EXCEPTION）。
 * <p>
 * 继承 SQLException，业务代码可捕获并处理。
 * 
 * @see cn.itcraft.jdbcmon.config.HugeResultSetAction#THROW_EXCEPTION
 */
public final class HugeResultSetException extends SQLException {

    private final String sql;
    private final int rowCount;
    private final int threshold;

    public HugeResultSetException(String sql, int rowCount, int threshold) {
        super(String.format("Huge ResultSet detected: %d rows (threshold: %d) for SQL: %s",
            rowCount, threshold, sql));
        this.sql = sql;
        this.rowCount = rowCount;
        this.threshold = threshold;
    }

    public String getSql() {
        return sql;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getThreshold() {
        return threshold;
    }
}