package cn.itcraft.jdbcmon.proxy.wrapper;

import cn.itcraft.jdbcmon.monitor.SqlMetrics;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

public final class MonitoredPreparedStatement implements PreparedStatement {

    private final PreparedStatement delegate;
    private final SqlMonitor monitor;
    private final String sql;
    private final SqlMetrics cachedMetrics;

    public MonitoredPreparedStatement(PreparedStatement delegate, SqlMonitor monitor, String sql) {
        this.delegate = delegate;
        this.monitor = monitor;
        this.sql = sql;
        this.cachedMetrics = monitor.getOrCreateMetrics(sql);
    }

    // ========== 监控方法 ==========

    @Override
    public boolean execute() throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            boolean result = delegate.execute();
            monitor.recordQueryFast(cachedMetrics, System.nanoTime() - start);
            return result;
        } catch (java.sql.SQLException e) {
            monitor.recordErrorFast(cachedMetrics, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public ResultSet executeQuery() throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            ResultSet rs = delegate.executeQuery();
            monitor.recordQueryFast(cachedMetrics, System.nanoTime() - start);
            return rs;
        } catch (java.sql.SQLException e) {
            monitor.recordErrorFast(cachedMetrics, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public int executeUpdate() throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            int rows = delegate.executeUpdate();
            monitor.recordUpdateFast(cachedMetrics, System.nanoTime() - start, rows);
            return rows;
        } catch (java.sql.SQLException e) {
            monitor.recordErrorFast(cachedMetrics, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public long executeLargeUpdate() throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            long rows = delegate.executeLargeUpdate();
            monitor.recordUpdateFast(cachedMetrics, System.nanoTime() - start, (int) Math.min(rows, Integer.MAX_VALUE));
            return rows;
        } catch (java.sql.SQLException e) {
            monitor.recordErrorFast(cachedMetrics, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public int[] executeBatch() throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            int[] rows = delegate.executeBatch();
            monitor.recordBatchFast(cachedMetrics, System.nanoTime() - start, rows);
            return rows;
        } catch (java.sql.SQLException e) {
            monitor.recordErrorFast(cachedMetrics, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public long[] executeLargeBatch() throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            long[] rows = delegate.executeLargeBatch();
            int[] intRows = new int[rows.length];
            for (int i = 0; i < rows.length; i++) {
                intRows[i] = (int) Math.min(rows[i], Integer.MAX_VALUE);
            }
            monitor.recordBatchFast(cachedMetrics, System.nanoTime() - start, intRows);
            return rows;
        } catch (java.sql.SQLException e) {
            monitor.recordErrorFast(cachedMetrics, System.nanoTime() - start, e);
            throw e;
        }
    }

    // ========== 非监控方法：直接委托 ==========

    @Override
    public void addBatch() throws java.sql.SQLException {
        delegate.addBatch();
    }

    @Override
    public void clearParameters() throws java.sql.SQLException {
        delegate.clearParameters();
    }

    @Override
    public void close() throws java.sql.SQLException {
        delegate.close();
    }

    @Override
    public void cancel() throws java.sql.SQLException {
        delegate.cancel();
    }

    @Override
    public void clearBatch() throws java.sql.SQLException {
        delegate.clearBatch();
    }

    @Override
    public Connection getConnection() throws java.sql.SQLException {
        return delegate.getConnection();
    }

    @Override
    public ResultSet getGeneratedKeys() throws java.sql.SQLException {
        return delegate.getGeneratedKeys();
    }

    @Override
    public ResultSetMetaData getMetaData() throws java.sql.SQLException {
        return delegate.getMetaData();
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws java.sql.SQLException {
        return delegate.getParameterMetaData();
    }

    @Override
    public int getUpdateCount() throws java.sql.SQLException {
        return delegate.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws java.sql.SQLException {
        return delegate.getMoreResults();
    }

    @Override
    public boolean getMoreResults(int current) throws java.sql.SQLException {
        return delegate.getMoreResults(current);
    }

    @Override
    public ResultSet getResultSet() throws java.sql.SQLException {
        return delegate.getResultSet();
    }

    @Override
    public int getResultSetConcurrency() throws java.sql.SQLException {
        return delegate.getResultSetConcurrency();
    }

    @Override
    public int getResultSetHoldability() throws java.sql.SQLException {
        return delegate.getResultSetHoldability();
    }

    @Override
    public int getResultSetType() throws java.sql.SQLException {
        return delegate.getResultSetType();
    }

    @Override
    public int getFetchDirection() throws java.sql.SQLException {
        return delegate.getFetchDirection();
    }

    @Override
    public void setFetchDirection(int direction) throws java.sql.SQLException {
        delegate.setFetchDirection(direction);
    }

    @Override
    public int getFetchSize() throws java.sql.SQLException {
        return delegate.getFetchSize();
    }

    @Override
    public void setFetchSize(int rows) throws java.sql.SQLException {
        delegate.setFetchSize(rows);
    }

    @Override
    public int getMaxFieldSize() throws java.sql.SQLException {
        return delegate.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws java.sql.SQLException {
        delegate.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws java.sql.SQLException {
        return delegate.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws java.sql.SQLException {
        delegate.setMaxRows(max);
    }

    @Override
    public long getLargeMaxRows() throws java.sql.SQLException {
        return delegate.getLargeMaxRows();
    }

    @Override
    public void setLargeMaxRows(long max) throws java.sql.SQLException {
        delegate.setLargeMaxRows(max);
    }

    @Override
    public int getQueryTimeout() throws java.sql.SQLException {
        return delegate.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws java.sql.SQLException {
        delegate.setQueryTimeout(seconds);
    }

    @Override
    public boolean isClosed() throws java.sql.SQLException {
        return delegate.isClosed();
    }

    @Override
    public boolean isPoolable() throws java.sql.SQLException {
        return delegate.isPoolable();
    }

    @Override
    public void setPoolable(boolean poolable) throws java.sql.SQLException {
        delegate.setPoolable(poolable);
    }

    @Override
    public void setCursorName(String name) throws java.sql.SQLException {
        delegate.setCursorName(name);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
        return delegate.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws java.sql.SQLException {
        delegate.setEscapeProcessing(enable);
    }

    // ========== 参数设置方法 ==========

    @Override
    public void setNull(int parameterIndex, int sqlType) throws java.sql.SQLException {
        delegate.setNull(parameterIndex, sqlType);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws java.sql.SQLException {
        delegate.setNull(parameterIndex, sqlType, typeName);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws java.sql.SQLException {
        delegate.setBoolean(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws java.sql.SQLException {
        delegate.setByte(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws java.sql.SQLException {
        delegate.setShort(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws java.sql.SQLException {
        delegate.setInt(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws java.sql.SQLException {
        delegate.setLong(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws java.sql.SQLException {
        delegate.setFloat(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws java.sql.SQLException {
        delegate.setDouble(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws java.sql.SQLException {
        delegate.setBigDecimal(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws java.sql.SQLException {
        delegate.setString(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws java.sql.SQLException {
        delegate.setBytes(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws java.sql.SQLException {
        delegate.setDate(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws java.sql.SQLException {
        delegate.setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws java.sql.SQLException {
        delegate.setTime(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws java.sql.SQLException {
        delegate.setTime(parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws java.sql.SQLException {
        delegate.setTimestamp(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws java.sql.SQLException {
        delegate.setTimestamp(parameterIndex, x, cal);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws java.sql.SQLException {
        delegate.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws java.sql.SQLException {
        delegate.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws java.sql.SQLException {
        delegate.setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws java.sql.SQLException {
        delegate.setUnicodeStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws java.sql.SQLException {
        delegate.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws java.sql.SQLException {
        delegate.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws java.sql.SQLException {
        delegate.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws java.sql.SQLException {
        delegate.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws java.sql.SQLException {
        delegate.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws java.sql.SQLException {
        delegate.setCharacterStream(parameterIndex, reader);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws java.sql.SQLException {
        delegate.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws java.sql.SQLException {
        delegate.setObject(parameterIndex, x);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws java.sql.SQLException {
        delegate.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setObject(int parameterIndex, Object x, SQLType targetSqlType, int scaleOrLength) throws java.sql.SQLException {
        delegate.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setObject(int parameterIndex, Object x, SQLType targetSqlType) throws java.sql.SQLException {
        delegate.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws java.sql.SQLException {
        delegate.setRef(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws java.sql.SQLException {
        delegate.setBlob(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws java.sql.SQLException {
        delegate.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws java.sql.SQLException {
        delegate.setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws java.sql.SQLException {
        delegate.setClob(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws java.sql.SQLException {
        delegate.setClob(parameterIndex, reader, length);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws java.sql.SQLException {
        delegate.setClob(parameterIndex, reader);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws java.sql.SQLException {
        delegate.setArray(parameterIndex, x);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws java.sql.SQLException {
        delegate.setURL(parameterIndex, x);
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws java.sql.SQLException {
        delegate.setRowId(parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws java.sql.SQLException {
        delegate.setNString(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws java.sql.SQLException {
        delegate.setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws java.sql.SQLException {
        delegate.setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws java.sql.SQLException {
        delegate.setNClob(parameterIndex, value);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws java.sql.SQLException {
        delegate.setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws java.sql.SQLException {
        delegate.setNClob(parameterIndex, reader);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws java.sql.SQLException {
        delegate.setSQLXML(parameterIndex, xmlObject);
    }

    // ========== Statement 接口的 execute/update 方法（带 SQL 参数）==========

    @Override
    public boolean execute(String sql) throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            boolean result = delegate.execute(sql);
            monitor.recordQuery(sql, System.nanoTime() - start);
            return result;
        } catch (java.sql.SQLException e) {
            monitor.recordError(sql, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            boolean result = delegate.execute(sql, autoGeneratedKeys);
            monitor.recordQuery(sql, System.nanoTime() - start);
            return result;
        } catch (java.sql.SQLException e) {
            monitor.recordError(sql, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            boolean result = delegate.execute(sql, columnIndexes);
            monitor.recordQuery(sql, System.nanoTime() - start);
            return result;
        } catch (java.sql.SQLException e) {
            monitor.recordError(sql, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            boolean result = delegate.execute(sql, columnNames);
            monitor.recordQuery(sql, System.nanoTime() - start);
            return result;
        } catch (java.sql.SQLException e) {
            monitor.recordError(sql, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public ResultSet executeQuery(String sql) throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            ResultSet rs = delegate.executeQuery(sql);
            monitor.recordQuery(sql, System.nanoTime() - start);
            return rs;
        } catch (java.sql.SQLException e) {
            monitor.recordError(sql, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public int executeUpdate(String sql) throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            int rows = delegate.executeUpdate(sql);
            monitor.recordUpdate(sql, System.nanoTime() - start, rows);
            return rows;
        } catch (java.sql.SQLException e) {
            monitor.recordError(sql, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            int rows = delegate.executeUpdate(sql, autoGeneratedKeys);
            monitor.recordUpdate(sql, System.nanoTime() - start, rows);
            return rows;
        } catch (java.sql.SQLException e) {
            monitor.recordError(sql, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            int rows = delegate.executeUpdate(sql, columnIndexes);
            monitor.recordUpdate(sql, System.nanoTime() - start, rows);
            return rows;
        } catch (java.sql.SQLException e) {
            monitor.recordError(sql, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws java.sql.SQLException {
        long start = System.nanoTime();
        try {
            int rows = delegate.executeUpdate(sql, columnNames);
            monitor.recordUpdate(sql, System.nanoTime() - start, rows);
            return rows;
        } catch (java.sql.SQLException e) {
            monitor.recordError(sql, System.nanoTime() - start, e);
            throw e;
        }
    }

    @Override
    public void addBatch(String sql) throws java.sql.SQLException {
        delegate.addBatch(sql);
    }

    @Override
    public void closeOnCompletion() throws java.sql.SQLException {
        delegate.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws java.sql.SQLException {
        return delegate.isCloseOnCompletion();
    }

    // ========== Object 方法 ==========

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return "MonitoredPreparedStatement[" + delegate + "]";
    }

    // ========== JDBC 4.2+ 方法 ==========

    public java.sql.SQLWarning getWarnings() throws java.sql.SQLException {
        return delegate.getWarnings();
    }

    public void clearWarnings() throws java.sql.SQLException {
        delegate.clearWarnings();
    }
}