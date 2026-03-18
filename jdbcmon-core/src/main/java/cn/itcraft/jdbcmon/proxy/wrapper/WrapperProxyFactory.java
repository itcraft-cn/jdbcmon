package cn.itcraft.jdbcmon.proxy.wrapper;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.spi.JdbcProxyFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public final class WrapperProxyFactory implements JdbcProxyFactory {

    public static final String NAME = "WRAPPER";

    @Override
    public Connection wrapConnection(Connection conn, SqlMonitor monitor, ProxyConfig config) {
        return new MonitoredConnection(conn, monitor, config);
    }

    @Override
    public Statement wrapStatement(Statement stmt, SqlMonitor monitor, ProxyConfig config, long parentProxyId) {
        return new MonitoredStatement(stmt, monitor);
    }

    @Override
    public PreparedStatement wrapPreparedStatement(PreparedStatement stmt, String sql, SqlMonitor monitor, ProxyConfig config, long parentProxyId) {
        return new MonitoredPreparedStatement(stmt, monitor, sql);
    }

    @Override
    public CallableStatement wrapCallableStatement(CallableStatement stmt, String sql, SqlMonitor monitor, ProxyConfig config, long parentProxyId) {
        return new MonitoredCallableStatement(stmt, monitor, sql);
    }

    @Override
    public String getName() {
        return NAME;
    }
}