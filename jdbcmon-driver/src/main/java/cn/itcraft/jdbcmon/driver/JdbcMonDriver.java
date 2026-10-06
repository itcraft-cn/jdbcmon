package cn.itcraft.jdbcmon.driver;

import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.wrap.MonitoredConnection;
import cn.itcraft.jdbcmon.wrap.WrappedFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

/**
 * jdbcmon 监控代理 Driver（模式2：Driver/URL 代理）
 * <p>
 * 通过在 JDBC URL 前追加 {@link DriverUrlParser#URL_PREFIX} 前缀即可零代码接入监控，
 * 内部剥离前缀后交由真实驱动建立连接，并将连接包装为监控代理，复用 jdbcmon-core 引擎。
 *
 * <pre>
 *   jdbc:jdbcmon:mysql://host:3306/db
 *   jdbc:jdbcmon:h2:mem:test
 * </pre>
 *
 * <p>注册方式：
 * <ul>
 *   <li>SPI 自动注册：classpath 存在 META-INF/services/java.sql.Driver</li>
 *   <li>显式指定：连接池 driverClassName 设为 {@code cn.itcraft.jdbcmon.driver.JdbcMonDriver}</li>
 * </ul>
 *
 * <p>注意事项：
 * <ul>
 *   <li>真实驱动必须对 DriverManager 的调用方 ClassLoader 可见；App 容器多 ClassLoader
 *       场景请显式配置真实驱动类名</li>
 *   <li>对已是 {@link MonitoredConnection} 的连接不再重复包装，保证幂等</li>
 * </ul>
 */
public final class JdbcMonDriver implements Driver {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMonDriver.class);

    /**
     * 监控代理 URL 前缀
     */
    public static final String URL_PREFIX = DriverUrlParser.URL_PREFIX;

    /*
     * 自身注册：JDBC 规范下，SPI 文件（META-INF/services/java.sql.Driver）仅触发驱动类加载，
     * 真正登记到 DriverManager 依赖驱动静态块中的 registerDriver 调用。
     */
    static {
        try {
            DriverManager.registerDriver(new JdbcMonDriver());
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }

        String realUrl = DriverUrlParser.toRealUrl(url);
        Connection real = openRealConnection(realUrl, info);
        if (real == null) {
            return null;
        }

        if (real instanceof MonitoredConnection) {
            LOGGER.debug("Connection already monitored, skip wrapping: {}", realUrl);
            return real;
        }

        SqlMonitor monitor = JdbcMonDriverConfig.getMonitor();
        LOGGER.debug("Wrapping connection with jdbcmon: {}", realUrl);
        return WrappedFactory.wrapConnection(real, monitor, JdbcMonDriverConfig.getConfig());
    }

    private Connection openRealConnection(String realUrl, Properties info) throws SQLException {
        if (info == null || info.isEmpty()) {
            return DriverManager.getConnection(realUrl);
        }
        return DriverManager.getConnection(realUrl, info);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return DriverUrlParser.isMonitoredUrl(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public java.util.logging.Logger getParentLogger() {
        return java.util.logging.Logger.getLogger("cn.itcraft.jdbcmon.driver");
    }
}
