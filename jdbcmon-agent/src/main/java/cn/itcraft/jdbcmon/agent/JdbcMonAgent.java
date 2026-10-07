package cn.itcraft.jdbcmon.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.util.jar.JarFile;

/**
 * jdbcmon javaagent 入口（模式3：零侵入）
 * <p>
 * 通过 {@code -javaagent:jdbcmon-agent.jar[=k=v;k=v]} 启动，进程启动期（premain）安装字节码增强，
 * 拦截 JDBC 驱动 {@code connect} 调用并包装连接，复用 jdbcmon-core 引擎。
 *
 * <p>注意事项：
 * <ul>
 *   <li>同时支持 premain 与 agentmain（动态 attach）</li>
 *   <li>agent 自身类需对被增强类可见；此处将 agent jar 追加到系统类加载器搜索路径</li>
 *   <li>Web 容器多 ClassLoader 场景下，子加载器中的驱动可能无法解析 agent 类，属已知限制</li>
 * </ul>
 */
public final class JdbcMonAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMonAgent.class);

    private JdbcMonAgent() {
    }

    /**
     * 进程启动期入口
     *
     * @param agentArgs agent 参数
     * @param instrumentation JVM Instrumentation
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        start(agentArgs, instrumentation);
    }

    /**
     * 动态 attach 入口
     *
     * @param agentArgs agent 参数
     * @param instrumentation JVM Instrumentation
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        start(agentArgs, instrumentation);
    }

    private static void start(String agentArgs, Instrumentation instrumentation) {
        JdbcMonAgentConfig.applyArgs(agentArgs);
        ensureAgentVisible(instrumentation);
        AgentInstaller.install(instrumentation);
        LOGGER.info("jdbcmon agent installed");
    }

    private static void ensureAgentVisible(Instrumentation instrumentation) {
        try {
            URI location = JdbcMonAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File file = new File(location);
            if (file.isFile()) {
                instrumentation.appendToSystemClassLoaderSearch(new JarFile(file));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to append jdbcmon agent jar to system classpath", e);
        }
    }
}
