package cn.itcraft.jdbcmon.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * WrappedConfig 属性加载器
 * <p>
 * 从 {@link Properties} 构建 {@link WrappedConfig}，供非 Spring 的接入方式（Driver 代理、javaagent）复用。
 * 属性键统一以 {@link #PROP_PREFIX} 为前缀。
 *
 * <p>支持的键：
 * enableMonitoring、metricsLevel、sampleRate、slowQueryThresholdMs、logSlowQueries、
 * collectStackTrace、useAdaptiveThreshold、enableLogging、hugeResultSetThreshold、hugeResultSetAction。
 *
 * <p>注意事项：
 * <ul>
 *   <li>未提供的键沿用 {@link WrappedConfig.Builder} 的默认值</li>
 *   <li>属性值非法（如 sampleRate 越界）会抛出 {@link IllegalArgumentException}，由调用方决定回退策略</li>
 * </ul>
 */
public final class WrappedConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(WrappedConfigLoader.class);

    /**
     * 属性键前缀
     */
    public static final String PROP_PREFIX = "jdbcmon.";

    /**
     * 默认 classpath 资源名
     */
    public static final String DEFAULT_RESOURCE = "jdbcmon.properties";

    private WrappedConfigLoader() {
    }

    /**
     * 加载默认来源的属性：classpath {@link #DEFAULT_RESOURCE} 叠加系统属性
     *
     * @return 属性集合
     */
    public static Properties loadProperties() {
        return loadProperties(DEFAULT_RESOURCE);
    }

    /**
     * 加载指定 classpath 资源的属性，并叠加系统属性
     *
     * @param resource classpath 资源名
     * @return 属性集合
     */
    public static Properties loadProperties(String resource) {
        Properties props = new Properties();
        loadResource(props, resource);
        overlaySystemProperties(props);
        return props;
    }

    /**
     * 从默认来源构建配置
     *
     * @return WrappedConfig
     */
    public static WrappedConfig load() {
        return load(loadProperties());
    }

    /**
     * 从给定属性构建配置
     *
     * @param props 属性集合
     * @return WrappedConfig
     */
    public static WrappedConfig load(Properties props) {
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

    private static void loadResource(Properties props, String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = WrappedConfigLoader.class.getClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in != null) {
                props.load(in);
                LOGGER.info("Loaded jdbcmon config from classpath resource: {}", resource);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read classpath resource: {}", resource, e);
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
}
