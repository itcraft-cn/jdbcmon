package cn.itcraft.jdbcmon.spi;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public interface JdbcProxyFactory {

    Connection wrapConnection(Connection conn, SqlMonitor monitor, ProxyConfig config);

    Statement wrapStatement(Statement stmt, SqlMonitor monitor, ProxyConfig config, long parentProxyId);

    PreparedStatement wrapPreparedStatement(PreparedStatement stmt, String sql, SqlMonitor monitor, ProxyConfig config, long parentProxyId);

    CallableStatement wrapCallableStatement(CallableStatement stmt, String sql, SqlMonitor monitor, ProxyConfig config, long parentProxyId);

    String getName();
}