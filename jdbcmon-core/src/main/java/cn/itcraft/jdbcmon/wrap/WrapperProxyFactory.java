package cn.itcraft.jdbcmon.wrap;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public final class WrapperProxyFactory {

    public static final String NAME = "WRAPPER";

    public Connection wrapConnection(Connection conn, SqlMonitor monitor, ProxyConfig config) {
        return new MonitoredConnection(conn, monitor, config);
    }

    public Statement wrapStatement(Statement stmt, SqlMonitor monitor, ProxyConfig config, long parentProxyId) {
        return new MonitoredStatement(stmt, monitor);
    }

    public PreparedStatement wrapPreparedStatement(PreparedStatement stmt, String sql, SqlMonitor monitor, ProxyConfig config, long parentProxyId) {
        return new MonitoredPreparedStatement(stmt, monitor, sql);
    }

    public CallableStatement wrapCallableStatement(CallableStatement stmt, String sql, SqlMonitor monitor, ProxyConfig config, long parentProxyId) {
        return new MonitoredCallableStatement(stmt, monitor, sql);
    }

    public String getName() {
        return NAME;
    }
}