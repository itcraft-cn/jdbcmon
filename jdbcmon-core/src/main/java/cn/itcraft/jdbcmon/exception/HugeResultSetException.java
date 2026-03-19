package cn.itcraft.jdbcmon.exception;

import java.sql.SQLException;

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