package cn.itcraft.jdbcmon.proxy.reflection;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.core.ConnectionProxyHandler;
import cn.itcraft.jdbcmon.core.StatementProxyHandler;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.spi.JdbcProxyFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;

public final class ReflectionProxyFactory implements JdbcProxyFactory {

    public static final String NAME = "REFLECTION";

    private final AtomicLong proxyIdGenerator = new AtomicLong();

    @Override
    public Connection wrapConnection(Connection conn, SqlMonitor monitor, ProxyConfig config) {
        long proxyId = proxyIdGenerator.incrementAndGet();
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
            conn.getClass().getClassLoader(),
            new Class[]{Connection.class},
            new ConnectionProxyHandler(conn, monitor, config, proxyId)
        );
    }

    @Override
    public Statement wrapStatement(Statement stmt, SqlMonitor monitor, ProxyConfig config, long parentProxyId) {
        return (Statement) java.lang.reflect.Proxy.newProxyInstance(
            stmt.getClass().getClassLoader(),
            new Class[]{Statement.class},
            new StatementProxyHandler(stmt, monitor, config, parentProxyId)
        );
    }

    @Override
    public PreparedStatement wrapPreparedStatement(PreparedStatement stmt, String sql, SqlMonitor monitor, ProxyConfig config, long parentProxyId) {
        return (PreparedStatement) java.lang.reflect.Proxy.newProxyInstance(
            stmt.getClass().getClassLoader(),
            new Class[]{PreparedStatement.class},
            new StatementProxyHandler(stmt, monitor, config, parentProxyId, sql)
        );
    }

    @Override
    public CallableStatement wrapCallableStatement(CallableStatement stmt, String sql, SqlMonitor monitor, ProxyConfig config, long parentProxyId) {
        return (CallableStatement) java.lang.reflect.Proxy.newProxyInstance(
            stmt.getClass().getClassLoader(),
            new Class[]{CallableStatement.class},
            new StatementProxyHandler(stmt, monitor, config, parentProxyId, sql)
        );
    }

    @Override
    public String getName() {
        return NAME;
    }
}