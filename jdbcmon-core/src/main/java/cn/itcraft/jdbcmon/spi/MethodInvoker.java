package cn.itcraft.jdbcmon.spi;

import java.lang.reflect.Method;

public interface MethodInvoker {

    Object invoke(Object target, Object... args) throws Throwable;

    Method getMethod();

    static MethodInvoker create(Method method) {
        return cn.itcraft.jdbcmon.internal.Platform.createInvoker(method);
    }
}