package cn.itcraft.jdbcmon.wrap;

import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public final class WrappedFactory {

    private WrappedFactory() {
    }

    public static Connection wrapConnection(Connection conn, SqlMonitor monitor, WrappedConfig config) {
        return new MonitoredConnection(conn, monitor, config);
    }

    public static Statement wrapStatement(Statement stmt, SqlMonitor monitor, WrappedConfig config, long parentProxyId) {
        return new MonitoredStatement(stmt, monitor, config);
    }

    public static PreparedStatement wrapPreparedStatement(PreparedStatement stmt, String sql, SqlMonitor monitor, WrappedConfig config, long parentProxyId) {
        return new MonitoredPreparedStatement(stmt, monitor, sql, config);
    }

    public static CallableStatement wrapCallableStatement(CallableStatement stmt, String sql, SqlMonitor monitor, WrappedConfig config, long parentProxyId) {
        return new MonitoredCallableStatement(stmt, monitor, sql, config);
    }
}