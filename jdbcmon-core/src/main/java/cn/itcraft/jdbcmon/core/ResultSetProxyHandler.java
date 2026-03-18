package cn.itcraft.jdbcmon.core;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class ResultSetProxyHandler implements InvocationHandler {

    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private final ResultSet target;
    private final SqlMonitor sqlMonitor;
    private final ProxyConfig config;
    private final long parentProxyId;

    private final LongAdder rowCount = new LongAdder();
    private volatile boolean closed = false;

    public ResultSetProxyHandler(ResultSet target, SqlMonitor sqlMonitor, ProxyConfig config, long parentProxyId) {
        this.target = target;
        this.sqlMonitor = sqlMonitor;
        this.config = config;
        this.parentProxyId = parentProxyId;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (isObjectMethod(methodName)) {
            return handleObjectMethod(methodName, args);
        }

        if (methodName.equals("close")) {
            return handleClose(method, args);
        }

        if (methodName.equals("next")) {
            return handleNext(method, args);
        }

        if (methodName.equals("isClosed")) {
            return closed;
        }

        return method.invoke(target, args);
    }

    private boolean isObjectMethod(String methodName) {
        return methodName.equals("hashCode") || methodName.equals("equals") || methodName.equals("toString");
    }

    private Object handleObjectMethod(String methodName, Object[] args) {
        switch (methodName) {
            case "hashCode":
                return System.identityHashCode(target);
            case "equals":
                return args != null && args.length > 0 && args[0] == target;
            case "toString":
                return "ResultSetProxy[rows=" + rowCount.sum() + "]";
            default:
                return null;
        }
    }

    private Object handleNext(Method method, Object[] args) throws Throwable {
        Object result = method.invoke(target, args);

        if (Boolean.TRUE.equals(result)) {
            rowCount.increment();
        }

        return result;
    }

    private Object handleClose(Method method, Object[] args) throws Throwable {
        closed = true;
        return method.invoke(target, args);
    }

    public long getRowCount() {
        return rowCount.sum();
    }
}