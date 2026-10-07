package cn.itcraft.jdbcmon.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.sql.Driver;

/**
 * 字节码增强安装器
 * <p>
 * 拦截所有 {@link java.sql.Driver} 实现类的 {@code connect(String, Properties)} 方法，
 * 在返回处包装连接。选择该拦截点可同时覆盖 DriverManager 与连接池两条路径
 * （连接池创建物理连接最终都会调用驱动 {@code connect}），且天然避免重复包装。
 */
final class AgentInstaller {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentInstaller.class);

    private AgentInstaller() {
    }

    static void install(Instrumentation instrumentation) {
        new AgentBuilder.Default()
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .with(new AgentBuilder.Listener.Adapter() {
                @Override
                public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
                        JavaModule module, boolean loaded, DynamicType dynamicType) {
                    LOGGER.info("jdbcmon transformed driver: {}", typeDescription.getName());
                }

                @Override
                public void onError(String typeName, ClassLoader classLoader, JavaModule module,
                        boolean loaded, Throwable throwable) {
                    LOGGER.warn("jdbcmon transform error on: {}", typeName, throwable);
                }
            })
            .ignore(ElementMatchers.nameStartsWith("net.bytebuddy.")
                .or(ElementMatchers.nameStartsWith("cn.itcraft.jdbcmon."))
                .or(ElementMatchers.nameStartsWith("cn.itcraft.jdbcmon.agent.shaded.")))
            .type(ElementMatchers.isSubTypeOf(Driver.class)
                .and(ElementMatchers.not(ElementMatchers.isInterface())))
            .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                builder.visit(Advice.to(ConnectAdvice.class)
                    .on(ElementMatchers.named("connect")
                        .and(ElementMatchers.takesArguments(2)))))
            .installOn(instrumentation);
    }
}
