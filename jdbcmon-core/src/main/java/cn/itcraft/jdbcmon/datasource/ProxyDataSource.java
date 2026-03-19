package cn.itcraft.jdbcmon.datasource;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.wrap.WrapperProxyFactory;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

public final class ProxyDataSource implements DataSource {

    private final DataSource target;
    private final SqlMonitor sqlMonitor;
    private final ProxyConfig config;
    private final WrapperProxyFactory proxyFactory;
    private final AtomicLong proxyIdGenerator = new AtomicLong();

    public ProxyDataSource(DataSource target, ProxyConfig config) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.config = config != null ? config : new ProxyConfig.Builder().build();
        this.sqlMonitor = new SqlMonitor(this.config);
        this.proxyFactory = new WrapperProxyFactory();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = target.getConnection();
        return wrapConnection(conn);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = target.getConnection(username, password);
        return wrapConnection(conn);
    }

    private Connection wrapConnection(Connection conn) {
        if (conn == null) {
            return null;
        }
        long proxyId = proxyIdGenerator.incrementAndGet();
        return proxyFactory.wrapConnection(conn, sqlMonitor, config);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return target.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        target.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        target.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return target.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return target.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return target.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || target.isWrapperFor(iface);
    }

    public SqlMonitor getSqlMonitor() {
        return sqlMonitor;
    }

    public DataSource getTargetDataSource() {
        return target;
    }

    public ProxyConfig getConfig() {
        return config;
    }

    public void shutdown() {
        sqlMonitor.shutdown();
    }
}