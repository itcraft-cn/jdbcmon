package cn.itcraft.jdbcmon.wrap;

import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

/**
 * 监控代理 DataSource
 * <p>
 * 包装目标 DataSource，返回监控代理 Connection，实现对 JDBC 调用的透明监控。
 * <p>
 * <h3>使用示例</h3>
 * <pre>{@code
 * DataSource originalDataSource = ...;
 * WrappedConfig config = new WrappedConfig.Builder()
 *     .sampleRate(10000)  // 全量采样
 *     .build();
 * 
 * DataSource monitoredDataSource = new WrappedDataSource(originalDataSource, config);
 * 
 * // 使用方式与原始 DataSource 完全相同
 * try (Connection conn = monitoredDataSource.getConnection()) {
 *     // 所有 JDBC 操作自动被监控
 * }
 * }</pre>
 * 
 * <h3>设计特点</h3>
 * <ul>
 *   <li>零侵入：业务代码无需修改</li>
 *   <li>透明代理：完全实现 DataSource 接口</li>
 *   <li>可配置：支持采样率、慢查询阈值等配置</li>
 * </ul>
 * 
 * @see WrappedConfig
 * @see SqlMonitor
 */
public final class WrappedDataSource implements DataSource {

    private final DataSource target;
    private final SqlMonitor sqlMonitor;
    private final WrappedConfig config;
    private final AtomicLong proxyIdGenerator = new AtomicLong();

    public WrappedDataSource(DataSource target, WrappedConfig config) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.config = config != null ? config : new WrappedConfig.Builder().build();
        this.sqlMonitor = new SqlMonitor(this.config);
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
        return WrappedFactory.wrapConnection(conn, sqlMonitor, config);
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

    public WrappedConfig getConfig() {
        return config;
    }

    public void shutdown() {
        sqlMonitor.shutdown();
    }
}