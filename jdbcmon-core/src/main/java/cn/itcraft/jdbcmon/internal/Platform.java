package cn.itcraft.jdbcmon.internal;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.spi.AsyncExecutor;
import cn.itcraft.jdbcmon.spi.MethodInvoker;
import cn.itcraft.jdbcmon.spi.VarAccessor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public final class Platform {

    private static final int JVM_VERSION;

    static {
        JVM_VERSION = detectJvmVersion();
    }

    private Platform() {
    }

    public static int getJvmVersion() {
        return JVM_VERSION;
    }

    public static MethodInvoker createInvoker(Method method) {
        return MethodHandleInvoker.create(method);
    }

    public static AsyncExecutor createAsyncExecutor(ProxyConfig config) {
        if (JVM_VERSION >= 23) {
            return createVirtualThreadExecutor();
        }
        return new PlatformThreadExecutor(config);
    }

    @SuppressWarnings("unchecked")
    public static <T> VarAccessor<T> createVarAccessor(Class<T> type, T initialValue) {
        if (JVM_VERSION >= 17) {
            return createVarHandleAccessor(type, initialValue);
        }
        return createAtomicAccessor(type, initialValue);
    }

    private static int detectJvmVersion() {
        String version = System.getProperty("java.version");
        if (version == null || version.isEmpty()) {
            return 8;
        }

        try {
            if (version.startsWith("1.")) {
                return Integer.parseInt(version.substring(2, 3));
            }

            int dotIndex = version.indexOf('.');
            if (dotIndex > 0) {
                return Integer.parseInt(version.substring(0, dotIndex));
            }

            int plusIndex = version.indexOf('+');
            if (plusIndex > 0) {
                return Integer.parseInt(version.substring(0, plusIndex));
            }

            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    private static AsyncExecutor createVirtualThreadExecutor() {
        try {
            Class<?> executorClass = Class.forName(
                "cn.itcraft.jdbcmon.internal.VirtualThreadExecutor");
            return (AsyncExecutor) executorClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return new PlatformThreadExecutor(null);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> VarAccessor<T> createVarHandleAccessor(Class<T> type, T initialValue) {
        try {
            Class<?> accessorClass = Class.forName(
                "cn.itcraft.jdbcmon.internal.VarHandleAccessor");
            java.lang.reflect.Constructor<?> constructor = 
                accessorClass.getDeclaredConstructor(Class.class, Object.class);
            return (VarAccessor<T>) constructor.newInstance(type, initialValue);
        } catch (Exception e) {
            return createAtomicAccessor(type, initialValue);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> VarAccessor<T> createAtomicAccessor(Class<T> type, T initialValue) {
        return new AtomicReferenceAccessor<>(initialValue);
    }
}