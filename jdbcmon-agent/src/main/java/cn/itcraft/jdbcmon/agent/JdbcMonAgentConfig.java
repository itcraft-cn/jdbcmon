package cn.itcraft.jdbcmon.agent;

import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.config.WrappedConfigLoader;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * javaagent 模式的全局配置与监控器持有者
 * <p>
 * 配置来源优先级（后者覆盖前者）：
 * <ol>
 *   <li>classpath 下的 {@code jdbcmon.properties}</li>
 *   <li>系统属性（{@code jdbcmon.*} 前缀）</li>
 *   <li>agent 启动参数（{@code -javaagent:jdbcmon-agent.jar=k=v;k=v}）</li>
 *   <li>内置默认值</li>
 * </ol>
 *
 * <p>agent 启动参数中的键可不带前缀，例如 {@code sampleRate=10000;slowQueryThresholdMs=1000}。
 *
 * <p>注意事项：配置解析失败会记录告警并回退到默认配置，绝不因配置问题阻断 agent 安装。
 */
public final class JdbcMonAgentConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMonAgentConfig.class);

    private static volatile WrappedConfig config;
    private static volatile SqlMonitor monitor;

    static {
        reload();
    }

    private JdbcMonAgentConfig() {
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
     * 以编码方式覆盖配置（便于测试）
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
        applyProperties(WrappedConfigLoader.loadProperties());
    }

    /**
     * 应用 agent 启动参数（在 classpath 与系统属性之上叠加），并重建配置
     *
     * @param agentArgs agent 参数，形如 {@code k=v;k=v}，可为 null
     */
    public static synchronized void applyArgs(String agentArgs) {
        Properties props = WrappedConfigLoader.loadProperties();
        if (agentArgs != null && !agentArgs.trim().isEmpty()) {
            String[] pairs = agentArgs.split(";");
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = pair.substring(0, idx).trim();
                String value = pair.substring(idx + 1).trim();
                if (!key.startsWith(WrappedConfigLoader.PROP_PREFIX)) {
                    key = WrappedConfigLoader.PROP_PREFIX + key;
                }
                props.setProperty(key, value);
            }
        }
        applyProperties(props);
    }

    private static void applyProperties(Properties props) {
        WrappedConfig loaded;
        try {
            loaded = WrappedConfigLoader.load(props);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to load jdbcmon agent config, falling back to defaults", e);
            loaded = new WrappedConfig.Builder().build();
        }
        config = loaded;
        monitor = new SqlMonitor(loaded);
    }
}
