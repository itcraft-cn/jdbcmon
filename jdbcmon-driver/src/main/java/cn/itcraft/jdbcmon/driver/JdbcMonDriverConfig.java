package cn.itcraft.jdbcmon.driver;

import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.config.WrappedConfigLoader;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver 模式的全局配置与监控器持有者
 * <p>
 * java.sql.Driver 是全局单例语义，故此处维护单一 {@link WrappedConfig} 与 {@link SqlMonitor}。
 * 配置来源与解析规则见 {@link WrappedConfigLoader}（classpath {@code jdbcmon.properties}
 * 叠加系统属性 {@code jdbcmon.*}，未提供项沿用默认值）。
 *
 * <p>注意事项：配置解析失败会记录告警并回退到默认配置，绝不因配置问题阻断驱动加载。
 */
public final class JdbcMonDriverConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMonDriverConfig.class);

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
            loaded = WrappedConfigLoader.load();
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to load jdbcmon driver config, falling back to defaults", e);
            loaded = new WrappedConfig.Builder().build();
        }
        config = loaded;
        monitor = new SqlMonitor(loaded);
    }
}
