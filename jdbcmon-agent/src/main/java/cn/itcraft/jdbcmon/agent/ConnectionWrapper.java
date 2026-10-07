package cn.itcraft.jdbcmon.agent;

import cn.itcraft.jdbcmon.wrap.MonitoredConnection;
import cn.itcraft.jdbcmon.wrap.WrappedFactory;

import java.sql.Connection;

/**
 * Connection 包装入口（供 agent 注入的 Advice 调用）
 * <p>
 * 职责：
 * <ul>
 *   <li>幂等：已是 {@link MonitoredConnection} 直接返回，避免重复包装与重复计数</li>
 *   <li>安全：包装过程任何异常都不应向业务抛出，失败时返回原始连接</li>
 * </ul>
 */
public final class ConnectionWrapper {

    private ConnectionWrapper() {
    }

    /**
     * 将原始连接包装为监控代理连接
     *
     * @param connection 原始连接，可为 null
     * @return 监控代理连接；入参为 null 或已包装或包装失败时返回原连接
     */
    public static Connection wrap(Connection connection) {
        if (connection == null || connection instanceof MonitoredConnection) {
            return connection;
        }
        try {
            return WrappedFactory.wrapConnection(
                connection,
                JdbcMonAgentConfig.getMonitor(),
                JdbcMonAgentConfig.getConfig());
        } catch (Throwable t) {
            return connection;
        }
    }
}
