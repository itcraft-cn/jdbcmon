package cn.itcraft.jdbcmon.internal;

import cn.itcraft.jdbcmon.spi.MethodInvoker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;

final class MethodHandleInvoker implements MethodInvoker {

    private final Method method;
    private final MethodHandle handle;
    private final boolean isStatic;

    private MethodHandleInvoker(Method method, MethodHandle handle, boolean isStatic) {
        this.method = method;
        this.handle = handle;
        this.isStatic = isStatic;
    }

    static MethodHandleInvoker create(Method method) {
        Objects.requireNonNull(method, "method cannot be null");

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflect(method);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());
            return new MethodHandleInvoker(method, handle, isStatic);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to create method handle for: " + method, e);
        }
    }

    @Override
    public Object invoke(Object target, Object... args) throws Throwable {
        MethodHandle boundHandle = handle;

        if (!isStatic) {
            if (target == null) {
                throw new NullPointerException("target cannot be null for instance method");
            }
            boundHandle = handle.bindTo(target);
        }

        if (args == null || args.length == 0) {
            return boundHandle.invoke();
        }

        return boundHandle.invokeWithArguments(args);
    }

    @Override
    public Method getMethod() {
        return method;
    }
}