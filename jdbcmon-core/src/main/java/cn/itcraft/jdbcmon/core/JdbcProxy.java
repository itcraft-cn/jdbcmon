package cn.itcraft.jdbcmon.core;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.datasource.ProxyDataSource;
import cn.itcraft.jdbcmon.datasource.ProxyDataSourceBuilder;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public final class JdbcProxy {

    private JdbcProxy() {
    }

    public static DataSource wrapDataSource(DataSource dataSource) {
        return wrapDataSource(dataSource, null);
    }

    public static DataSource wrapDataSource(DataSource dataSource, ProxyConfig config) {
        Objects.requireNonNull(dataSource, "dataSource cannot be null");
        return ProxyDataSourceBuilder.create(dataSource)
            .config(config)
            .build();
    }

    public static Connection wrapConnection(Connection connection, SqlMonitor sqlMonitor, ProxyConfig config) {
        Objects.requireNonNull(connection, "connection cannot be null");
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
            connection.getClass().getClassLoader(),
            new Class[]{Connection.class},
            new ConnectionProxyHandler(connection, sqlMonitor, config, System.identityHashCode(connection))
        );
    }

    public static Statement wrapStatement(Statement statement, SqlMonitor sqlMonitor, ProxyConfig config) {
        Objects.requireNonNull(statement, "statement cannot be null");
        return (Statement) java.lang.reflect.Proxy.newProxyInstance(
            statement.getClass().getClassLoader(),
            new Class[]{Statement.class},
            new StatementProxyHandler(statement, sqlMonitor, config, System.identityHashCode(statement))
        );
    }
}