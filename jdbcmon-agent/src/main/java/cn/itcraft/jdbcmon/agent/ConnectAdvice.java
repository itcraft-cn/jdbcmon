package cn.itcraft.jdbcmon.agent;

import net.bytebuddy.asm.Advice;

import java.sql.Connection;

/**
 * {@code java.sql.Driver#connect} 的字节码增强建议
 * <p>
 * 在方法返回处将返回值替换为监控代理连接。该建议会被内联进被增强的驱动类，
 * 运行时仅依赖 {@link ConnectionWrapper}。
 *
 * <p>注意事项：以 {@code suppress = Throwable.class} 保证监控逻辑异常不影响驱动行为。
 */
public final class ConnectAdvice {

    private ConnectAdvice() {
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Return(readOnly = false) Connection connection) {
        connection = ConnectionWrapper.wrap(connection);
    }
}
