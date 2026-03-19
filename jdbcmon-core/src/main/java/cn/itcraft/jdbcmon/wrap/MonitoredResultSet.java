package cn.itcraft.jdbcmon.wrap;

import cn.itcraft.jdbcmon.config.HugeResultSetAction;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public final class MonitoredResultSet implements ResultSet {

    private static final ResultSetMonitor NOOP = ResultSetMonitor.NOOP;

    private final ResultSet delegate;
    private final ResultSetMonitor monitor;
    private boolean closed = false;

    public MonitoredResultSet(ResultSet delegate, SqlMonitor sqlMonitor, String sql,
            int threshold, HugeResultSetAction action) {
        this.delegate = delegate;
        this.monitor = ResultSetMonitors.create(sqlMonitor, sql, threshold, action);
    }

    @Override
    public boolean next() throws java.sql.SQLException {
        boolean hasNext = delegate.next();
        if (hasNext) {
            monitor.onRow();
        }
        return hasNext;
    }

    @Override
    public void close() throws java.sql.SQLException {
        if (!closed) {
            monitor.onClose();
            closed = true;
        }
        delegate.close();
    }

    @Override
    public boolean isClosed() throws java.sql.SQLException {
        return closed || delegate.isClosed();
    }

    @Override
    public int getRow() throws java.sql.SQLException {
        return delegate.getRow();
    }

    @Override
    public boolean wasNull() throws java.sql.SQLException {
        return delegate.wasNull();
    }

    @Override
    public String getString(int columnIndex) throws java.sql.SQLException {
        return delegate.getString(columnIndex);
    }

    @Override
    public boolean getBoolean(int columnIndex) throws java.sql.SQLException {
        return delegate.getBoolean(columnIndex);
    }

    @Override
    public byte getByte(int columnIndex) throws java.sql.SQLException {
        return delegate.getByte(columnIndex);
    }

    @Override
    public short getShort(int columnIndex) throws java.sql.SQLException {
        return delegate.getShort(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) throws java.sql.SQLException {
        return delegate.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) throws java.sql.SQLException {
        return delegate.getLong(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) throws java.sql.SQLException {
        return delegate.getFloat(columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) throws java.sql.SQLException {
        return delegate.getDouble(columnIndex);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws java.sql.SQLException {
        return delegate.getBytes(columnIndex);
    }

    @Override
    public Date getDate(int columnIndex) throws java.sql.SQLException {
        return delegate.getDate(columnIndex);
    }

    @Override
    public Time getTime(int columnIndex) throws java.sql.SQLException {
        return delegate.getTime(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws java.sql.SQLException {
        return delegate.getTimestamp(columnIndex);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws java.sql.SQLException {
        return delegate.getAsciiStream(columnIndex);
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws java.sql.SQLException {
        return delegate.getUnicodeStream(columnIndex);
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws java.sql.SQLException {
        return delegate.getBinaryStream(columnIndex);
    }

    @Override
    public String getString(String columnLabel) throws java.sql.SQLException {
        return delegate.getString(columnLabel);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws java.sql.SQLException {
        return delegate.getBoolean(columnLabel);
    }

    @Override
    public byte getByte(String columnLabel) throws java.sql.SQLException {
        return delegate.getByte(columnLabel);
    }

    @Override
    public short getShort(String columnLabel) throws java.sql.SQLException {
        return delegate.getShort(columnLabel);
    }

    @Override
    public int getInt(String columnLabel) throws java.sql.SQLException {
        return delegate.getInt(columnLabel);
    }

    @Override
    public long getLong(String columnLabel) throws java.sql.SQLException {
        return delegate.getLong(columnLabel);
    }

    @Override
    public float getFloat(String columnLabel) throws java.sql.SQLException {
        return delegate.getFloat(columnLabel);
    }

    @Override
    public double getDouble(String columnLabel) throws java.sql.SQLException {
        return delegate.getDouble(columnLabel);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws java.sql.SQLException {
        return delegate.getBytes(columnLabel);
    }

    @Override
    public Date getDate(String columnLabel) throws java.sql.SQLException {
        return delegate.getDate(columnLabel);
    }

    @Override
    public Time getTime(String columnLabel) throws java.sql.SQLException {
        return delegate.getTime(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws java.sql.SQLException {
        return delegate.getTimestamp(columnLabel);
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws java.sql.SQLException {
        return delegate.getAsciiStream(columnLabel);
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws java.sql.SQLException {
        return delegate.getUnicodeStream(columnLabel);
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws java.sql.SQLException {
        return delegate.getBinaryStream(columnLabel);
    }

    @Override
    public SQLWarning getWarnings() throws java.sql.SQLException {
        return delegate.getWarnings();
    }

    @Override
    public void clearWarnings() throws java.sql.SQLException {
        delegate.clearWarnings();
    }

    @Override
    public String getCursorName() throws java.sql.SQLException {
        return delegate.getCursorName();
    }

    @Override
    public ResultSetMetaData getMetaData() throws java.sql.SQLException {
        return delegate.getMetaData();
    }

    @Override
    public Object getObject(int columnIndex) throws java.sql.SQLException {
        return delegate.getObject(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel) throws java.sql.SQLException {
        return delegate.getObject(columnLabel);
    }

    @Override
    public int findColumn(String columnLabel) throws java.sql.SQLException {
        return delegate.findColumn(columnLabel);
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws java.sql.SQLException {
        return delegate.getCharacterStream(columnIndex);
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws java.sql.SQLException {
        return delegate.getCharacterStream(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws java.sql.SQLException {
        return delegate.getBigDecimal(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws java.sql.SQLException {
        return delegate.getBigDecimal(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws java.sql.SQLException {
        return delegate.getBigDecimal(columnIndex, scale);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws java.sql.SQLException {
        return delegate.getBigDecimal(columnLabel, scale);
    }

    @Override
    public boolean isBeforeFirst() throws java.sql.SQLException {
        return delegate.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast() throws java.sql.SQLException {
        return delegate.isAfterLast();
    }

    @Override
    public boolean isFirst() throws java.sql.SQLException {
        return delegate.isFirst();
    }

    @Override
    public boolean isLast() throws java.sql.SQLException {
        return delegate.isLast();
    }

    @Override
    public void beforeFirst() throws java.sql.SQLException {
        delegate.beforeFirst();
    }

    @Override
    public void afterLast() throws java.sql.SQLException {
        delegate.afterLast();
    }

    @Override
    public boolean first() throws java.sql.SQLException {
        return delegate.first();
    }

    @Override
    public boolean last() throws java.sql.SQLException {
        return delegate.last();
    }

    @Override
    public int getType() throws java.sql.SQLException {
        return delegate.getType();
    }

    @Override
    public int getConcurrency() throws java.sql.SQLException {
        return delegate.getConcurrency();
    }

    @Override
    public boolean rowUpdated() throws java.sql.SQLException {
        return delegate.rowUpdated();
    }

    @Override
    public boolean rowInserted() throws java.sql.SQLException {
        return delegate.rowInserted();
    }

    @Override
    public boolean rowDeleted() throws java.sql.SQLException {
        return delegate.rowDeleted();
    }

    @Override
    public void updateNull(int columnIndex) throws java.sql.SQLException {
        delegate.updateNull(columnIndex);
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws java.sql.SQLException {
        delegate.updateBoolean(columnIndex, x);
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws java.sql.SQLException {
        delegate.updateByte(columnIndex, x);
    }

    @Override
    public void updateShort(int columnIndex, short x) throws java.sql.SQLException {
        delegate.updateShort(columnIndex, x);
    }

    @Override
    public void updateInt(int columnIndex, int x) throws java.sql.SQLException {
        delegate.updateInt(columnIndex, x);
    }

    @Override
    public void updateLong(int columnIndex, long x) throws java.sql.SQLException {
        delegate.updateLong(columnIndex, x);
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws java.sql.SQLException {
        delegate.updateFloat(columnIndex, x);
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws java.sql.SQLException {
        delegate.updateDouble(columnIndex, x);
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws java.sql.SQLException {
        delegate.updateBigDecimal(columnIndex, x);
    }

    @Override
    public void updateString(int columnIndex, String x) throws java.sql.SQLException {
        delegate.updateString(columnIndex, x);
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws java.sql.SQLException {
        delegate.updateBytes(columnIndex, x);
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws java.sql.SQLException {
        delegate.updateDate(columnIndex, x);
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws java.sql.SQLException {
        delegate.updateTime(columnIndex, x);
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws java.sql.SQLException {
        delegate.updateTimestamp(columnIndex, x);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws java.sql.SQLException {
        delegate.updateAsciiStream(columnIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws java.sql.SQLException {
        delegate.updateBinaryStream(columnIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws java.sql.SQLException {
        delegate.updateCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws java.sql.SQLException {
        delegate.updateObject(columnIndex, x, scaleOrLength);
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws java.sql.SQLException {
        delegate.updateObject(columnIndex, x);
    }

    @Override
    public void updateNull(String columnLabel) throws java.sql.SQLException {
        delegate.updateNull(columnLabel);
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws java.sql.SQLException {
        delegate.updateBoolean(columnLabel, x);
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws java.sql.SQLException {
        delegate.updateByte(columnLabel, x);
    }

    @Override
    public void updateShort(String columnLabel, short x) throws java.sql.SQLException {
        delegate.updateShort(columnLabel, x);
    }

    @Override
    public void updateInt(String columnLabel, int x) throws java.sql.SQLException {
        delegate.updateInt(columnLabel, x);
    }

    @Override
    public void updateLong(String columnLabel, long x) throws java.sql.SQLException {
        delegate.updateLong(columnLabel, x);
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws java.sql.SQLException {
        delegate.updateFloat(columnLabel, x);
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws java.sql.SQLException {
        delegate.updateDouble(columnLabel, x);
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws java.sql.SQLException {
        delegate.updateBigDecimal(columnLabel, x);
    }

    @Override
    public void updateString(String columnLabel, String x) throws java.sql.SQLException {
        delegate.updateString(columnLabel, x);
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws java.sql.SQLException {
        delegate.updateBytes(columnLabel, x);
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws java.sql.SQLException {
        delegate.updateDate(columnLabel, x);
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws java.sql.SQLException {
        delegate.updateTime(columnLabel, x);
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws java.sql.SQLException {
        delegate.updateTimestamp(columnLabel, x);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws java.sql.SQLException {
        delegate.updateAsciiStream(columnLabel, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws java.sql.SQLException {
        delegate.updateBinaryStream(columnLabel, x, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws java.sql.SQLException {
        delegate.updateCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws java.sql.SQLException {
        delegate.updateObject(columnLabel, x, scaleOrLength);
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws java.sql.SQLException {
        delegate.updateObject(columnLabel, x);
    }

    @Override
    public void insertRow() throws java.sql.SQLException {
        delegate.insertRow();
    }

    @Override
    public void updateRow() throws java.sql.SQLException {
        delegate.updateRow();
    }

    @Override
    public void deleteRow() throws java.sql.SQLException {
        delegate.deleteRow();
    }

    @Override
    public void refreshRow() throws java.sql.SQLException {
        delegate.refreshRow();
    }

    @Override
    public void cancelRowUpdates() throws java.sql.SQLException {
        delegate.cancelRowUpdates();
    }

    @Override
    public void moveToInsertRow() throws java.sql.SQLException {
        delegate.moveToInsertRow();
    }

    @Override
    public void moveToCurrentRow() throws java.sql.SQLException {
        delegate.moveToCurrentRow();
    }

    @Override
    public Statement getStatement() throws java.sql.SQLException {
        return delegate.getStatement();
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws java.sql.SQLException {
        return delegate.getObject(columnIndex, map);
    }

    @Override
    public Ref getRef(int columnIndex) throws java.sql.SQLException {
        return delegate.getRef(columnIndex);
    }

    @Override
    public Blob getBlob(int columnIndex) throws java.sql.SQLException {
        return delegate.getBlob(columnIndex);
    }

    @Override
    public Clob getClob(int columnIndex) throws java.sql.SQLException {
        return delegate.getClob(columnIndex);
    }

    @Override
    public Array getArray(int columnIndex) throws java.sql.SQLException {
        return delegate.getArray(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws java.sql.SQLException {
        return delegate.getObject(columnLabel, map);
    }

    @Override
    public Ref getRef(String columnLabel) throws java.sql.SQLException {
        return delegate.getRef(columnLabel);
    }

    @Override
    public Blob getBlob(String columnLabel) throws java.sql.SQLException {
        return delegate.getBlob(columnLabel);
    }

    @Override
    public Clob getClob(String columnLabel) throws java.sql.SQLException {
        return delegate.getClob(columnLabel);
    }

    @Override
    public Array getArray(String columnLabel) throws java.sql.SQLException {
        return delegate.getArray(columnLabel);
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws java.sql.SQLException {
        return delegate.getDate(columnIndex, cal);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws java.sql.SQLException {
        return delegate.getDate(columnLabel, cal);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws java.sql.SQLException {
        return delegate.getTime(columnIndex, cal);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws java.sql.SQLException {
        return delegate.getTime(columnLabel, cal);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws java.sql.SQLException {
        return delegate.getTimestamp(columnIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws java.sql.SQLException {
        return delegate.getTimestamp(columnLabel, cal);
    }

    @Override
    public URL getURL(int columnIndex) throws java.sql.SQLException {
        return delegate.getURL(columnIndex);
    }

    @Override
    public URL getURL(String columnLabel) throws java.sql.SQLException {
        return delegate.getURL(columnLabel);
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws java.sql.SQLException {
        delegate.updateRef(columnIndex, x);
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws java.sql.SQLException {
        delegate.updateRef(columnLabel, x);
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws java.sql.SQLException {
        delegate.updateBlob(columnIndex, x);
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws java.sql.SQLException {
        delegate.updateBlob(columnLabel, x);
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws java.sql.SQLException {
        delegate.updateClob(columnIndex, x);
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws java.sql.SQLException {
        delegate.updateClob(columnLabel, x);
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws java.sql.SQLException {
        delegate.updateArray(columnIndex, x);
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws java.sql.SQLException {
        delegate.updateArray(columnLabel, x);
    }

    @Override
    public RowId getRowId(int columnIndex) throws java.sql.SQLException {
        return delegate.getRowId(columnIndex);
    }

    @Override
    public RowId getRowId(String columnLabel) throws java.sql.SQLException {
        return delegate.getRowId(columnLabel);
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws java.sql.SQLException {
        delegate.updateRowId(columnIndex, x);
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws java.sql.SQLException {
        delegate.updateRowId(columnLabel, x);
    }

    @Override
    public int getHoldability() throws java.sql.SQLException {
        return delegate.getHoldability();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
        return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return delegate.unwrap(iface);
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws java.sql.SQLException {
        delegate.updateNString(columnIndex, nString);
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws java.sql.SQLException {
        delegate.updateNString(columnLabel, nString);
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws java.sql.SQLException {
        delegate.updateNClob(columnIndex, nClob);
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws java.sql.SQLException {
        delegate.updateNClob(columnLabel, nClob);
    }

    @Override
    public NClob getNClob(int columnIndex) throws java.sql.SQLException {
        return delegate.getNClob(columnIndex);
    }

    @Override
    public NClob getNClob(String columnLabel) throws java.sql.SQLException {
        return delegate.getNClob(columnLabel);
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws java.sql.SQLException {
        return delegate.getSQLXML(columnIndex);
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws java.sql.SQLException {
        return delegate.getSQLXML(columnLabel);
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws java.sql.SQLException {
        delegate.updateSQLXML(columnIndex, xmlObject);
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws java.sql.SQLException {
        delegate.updateSQLXML(columnLabel, xmlObject);
    }

    @Override
    public String getNString(int columnIndex) throws java.sql.SQLException {
        return delegate.getNString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws java.sql.SQLException {
        return delegate.getNString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws java.sql.SQLException {
        return delegate.getNCharacterStream(columnIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws java.sql.SQLException {
        return delegate.getNCharacterStream(columnLabel);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws java.sql.SQLException {
        delegate.updateNCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws java.sql.SQLException {
        delegate.updateNCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws java.sql.SQLException {
        delegate.updateAsciiStream(columnIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws java.sql.SQLException {
        delegate.updateBinaryStream(columnIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws java.sql.SQLException {
        delegate.updateCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws java.sql.SQLException {
        delegate.updateAsciiStream(columnLabel, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws java.sql.SQLException {
        delegate.updateBinaryStream(columnLabel, x, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws java.sql.SQLException {
        delegate.updateCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws java.sql.SQLException {
        delegate.updateBlob(columnIndex, inputStream, length);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws java.sql.SQLException {
        delegate.updateBlob(columnLabel, inputStream, length);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws java.sql.SQLException {
        delegate.updateClob(columnIndex, reader, length);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws java.sql.SQLException {
        delegate.updateClob(columnLabel, reader, length);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws java.sql.SQLException {
        delegate.updateNClob(columnIndex, reader, length);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws java.sql.SQLException {
        delegate.updateNClob(columnLabel, reader, length);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws java.sql.SQLException {
        delegate.updateNCharacterStream(columnIndex, x);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws java.sql.SQLException {
        delegate.updateNCharacterStream(columnLabel, reader);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws java.sql.SQLException {
        delegate.updateAsciiStream(columnIndex, x);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws java.sql.SQLException {
        delegate.updateBinaryStream(columnIndex, x);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws java.sql.SQLException {
        delegate.updateCharacterStream(columnIndex, x);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws java.sql.SQLException {
        delegate.updateAsciiStream(columnLabel, x);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws java.sql.SQLException {
        delegate.updateBinaryStream(columnLabel, x);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws java.sql.SQLException {
        delegate.updateCharacterStream(columnLabel, reader);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws java.sql.SQLException {
        delegate.updateBlob(columnIndex, inputStream);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws java.sql.SQLException {
        delegate.updateBlob(columnLabel, inputStream);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws java.sql.SQLException {
        delegate.updateClob(columnIndex, reader);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws java.sql.SQLException {
        delegate.updateClob(columnLabel, reader);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws java.sql.SQLException {
        delegate.updateNClob(columnIndex, reader);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws java.sql.SQLException {
        delegate.updateNClob(columnLabel, reader);
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws java.sql.SQLException {
        return delegate.getObject(columnIndex, type);
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws java.sql.SQLException {
        return delegate.getObject(columnLabel, type);
    }

    @Override
    public void updateObject(int columnIndex, Object x, java.sql.SQLType targetSqlType, int scaleOrLength) throws java.sql.SQLException {
        delegate.updateObject(columnIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void updateObject(String columnLabel, Object x, java.sql.SQLType targetSqlType, int scaleOrLength) throws java.sql.SQLException {
        delegate.updateObject(columnLabel, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void updateObject(int columnIndex, Object x, java.sql.SQLType targetSqlType) throws java.sql.SQLException {
        delegate.updateObject(columnIndex, x, targetSqlType);
    }

    @Override
    public void updateObject(String columnLabel, Object x, java.sql.SQLType targetSqlType) throws java.sql.SQLException {
        delegate.updateObject(columnLabel, x, targetSqlType);
    }

    @Override
    public boolean absolute(int row) throws java.sql.SQLException {
        return delegate.absolute(row);
    }

    @Override
    public boolean relative(int rows) throws java.sql.SQLException {
        return delegate.relative(rows);
    }

    @Override
    public boolean previous() throws java.sql.SQLException {
        return delegate.previous();
    }

    @Override
    public void setFetchDirection(int direction) throws java.sql.SQLException {
        delegate.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws java.sql.SQLException {
        return delegate.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws java.sql.SQLException {
        delegate.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws java.sql.SQLException {
        return delegate.getFetchSize();
    }
}