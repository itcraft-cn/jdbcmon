package cn.itcraft.jdbcmon.driver;

import cn.itcraft.jdbcmon.config.HugeResultSetAction;
import cn.itcraft.jdbcmon.config.MetricsLevel;
import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Driver 模式的全局配置与监控器持有者
 * <p>
 * java.sql.Driver 是全局单例语义，故此处维护单一 {@link WrappedConfig} 与 {@link SqlMonitor}。
 * 配置来源优先级（后者覆盖前者）：
 * <ol>
 *   <li>classpath 下的 {@code jdbcmon.properties}</li>
 *   <li>系统属性（{@code jdbcmon.*} 前缀）</li>
 *   <li>内置默认值</li>
 * </ol>
 *
 * <p>支持的键（均带 {@code jdbcmon.} 前缀）：
 * enableMonitoring、metricsLevel、sampleRate、slowQueryThresholdMs、logSlowQueries、
 * collectStackTrace、useAdaptiveThreshold、enableLogging、hugeResultSetThreshold、hugeResultSetAction。
 *
 * <p>注意事项：配置解析失败会记录告警并回退到默认配置，绝不因配置问题阻断驱动加载。
 */
public final class JdbcMonDriverConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMonDriverConfig.class);

    private static final String RESOURCE = "jdbcmon.properties";
    private static final String PROP_PREFIX = "jdbcmon.";

    private static volatile WrappedConfig config;
    private static volatile SqlMonitor monitor;

    static {
        reload();
    }

    private JdbcMonDriverConfig() {
    }

    /**
     * 获取全局监控配置
     *
     * @return 配置实例
     */
    public static WrappedConfig getConfig() {
        return config;
    }

    /**
     * 获取全局监控器
     *
     * @return SqlMonitor 单例
     */
    public static SqlMonitor getMonitor() {
        return monitor;
    }

    /**
     * 以编码方式覆盖配置（便于测试或嵌入式场景）
     *
     * @param newConfig 新配置，不可为 null
     * @throws IllegalArgumentException newConfig 为 null
     */
    public static synchronized void setConfig(WrappedConfig newConfig) {
        if (newConfig == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        config = newConfig;
        monitor = new SqlMonitor(newConfig);
    }

    /**
     * 依据 classpath 资源与系统属性重新加载配置
     */
    public static synchronized void reload() {
        WrappedConfig loaded;
        try {
            loaded = build(loadProperties());
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to load jdbcmon driver config, falling back to defaults", e);
            loaded = new WrappedConfig.Builder().build();
        }
        config = loaded;
        monitor = new SqlMonitor(loaded);
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        loadResource(props);
        overlaySystemProperties(props);
        return props;
    }

    private static void loadResource(Properties props) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = JdbcMonDriverConfig.class.getClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                props.load(in);
                LOGGER.info("Loaded jdbcmon driver config from classpath resource: {}", RESOURCE);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read classpath resource: {}", RESOURCE, e);
        }
    }

    private static void overlaySystemProperties(Properties props) {
        Properties sys = System.getProperties();
        for (String name : sys.stringPropertyNames()) {
            if (name.startsWith(PROP_PREFIX)) {
                props.setProperty(name, sys.getProperty(name));
            }
        }
    }

    private static WrappedConfig build(Properties props) {
        WrappedConfig.Builder builder = new WrappedConfig.Builder();

        String value = props.getProperty(PROP_PREFIX + "enableMonitoring");
        if (value != null) {
            builder.enableMonitoring(Boolean.parseBoolean(value.trim()));
        }

        value = props.getProperty(PROP_PREFIX + "metricsLevel");
        if (value != null) {
            builder.metricsLevel(MetricsLevel.valueOf(value.trim().toUpperCase()));
        }

        value = props.getProperty(PROP_PREFIX + "sampleRate");
        if (value != null) {
            builder.sampleRate(Integer.parseInt(value.trim()));
        }

        value = props.getProperty(PROP_PREFIX + "slowQueryThresholdMs");
        if (value != null) {
            builder.slowQueryThresholdMs(Long.parseLong(value.trim()));
        }

        value = props.getProperty(PROP_PREFIX + "logSlowQueries");
        if (value != null) {
            builder.logSlowQueries(Boolean.parseBoolean(value.trim()));
        }

        value = props.getProperty(PROP_PREFIX + "collectStackTrace");
        if (value != null) {
            builder.collectStackTrace(Boolean.parseBoolean(value.trim()));
        }

        value = props.getProperty(PROP_PREFIX + "useAdaptiveThreshold");
        if (value != null) {
            builder.useAdaptiveThreshold(Boolean.parseBoolean(value.trim()));
        }

        value = props.getProperty(PROP_PREFIX + "enableLogging");
        if (value != null) {
            builder.enableLogging(Boolean.parseBoolean(value.trim()));
        }

        value = props.getProperty(PROP_PREFIX + "hugeResultSetThreshold");
        if (value != null) {
            builder.hugeResultSetThreshold(Integer.parseInt(value.trim()));
        }

        value = props.getProperty(PROP_PREFIX + "hugeResultSetAction");
        if (value != null) {
            builder.hugeResultSetAction(HugeResultSetAction.valueOf(value.trim().toUpperCase()));
        }

        return builder.build();
    }
}
