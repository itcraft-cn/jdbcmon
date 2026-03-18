package cn.itcraft.jdbcmon.core;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StatementProxyHandler implements InvocationHandler {

    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private final Statement target;
    private final SqlMonitor sqlMonitor;
    private final ProxyConfig config;
    private final long parentProxyId;
    private final String preparedSql;

    public StatementProxyHandler(Statement target, SqlMonitor sqlMonitor, ProxyConfig config, long parentProxyId) {
        this(target, sqlMonitor, config, parentProxyId, null);
    }

    public StatementProxyHandler(Statement target, SqlMonitor sqlMonitor, ProxyConfig config, long parentProxyId, String preparedSql) {
        this.target = target;
        this.sqlMonitor = sqlMonitor;
        this.config = config;
        this.parentProxyId = parentProxyId;
        this.preparedSql = preparedSql;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (isObjectMethod(methodName)) {
            return handleObjectMethod(methodName, args);
        }

        if (methodName.equals("close")) {
            return method.invoke(target, args);
        }

        if (methodName.startsWith("execute") || methodName.startsWith("update") || methodName.startsWith("query")) {
            return handleExecute(method, args);
        }

        if (methodName.equals("addBatch")) {
            return handleAddBatch(method, args);
        }

        if (methodName.equals("executeBatch") || methodName.equals("executeLargeBatch")) {
            return handleExecuteBatch(method, args);
        }

        if (methodName.equals("getResultSet")) {
            return method.invoke(target, args);
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
                return "StatementProxy[" + target + "]";
            default:
                return null;
        }
    }

    private Object handleExecute(Method method, Object[] args) throws Throwable {
        SqlExecutionContext context = SqlExecutionContext.acquire();

        try {
            String sql = extractSql(args);

            context.setProxyId(parentProxyId);
            context.setMethodName(method.getName());
            context.setClassName("Statement");
            context.setSql(sql);
            context.setThreadName(Thread.currentThread().getName());
            context.setThreadId(Thread.currentThread().getId());
            context.setStartTimeNanos(System.nanoTime());

            Object result = method.invoke(target, args);

            result = wrapResultIfNeeded(result, method);

            long elapsedNanos = System.nanoTime() - context.getStartTimeNanos();
            if (sqlMonitor != null) {
                sqlMonitor.recordSuccess(context, elapsedNanos, result);
            }

            return result;

        } catch (Throwable t) {
            long elapsedNanos = System.nanoTime() - context.getStartTimeNanos();
            if (sqlMonitor != null) {
                sqlMonitor.recordFailure(context, elapsedNanos, t);
            }
            throw t;
        } finally {
            SqlExecutionContext.release(context);
        }
    }

    private String extractSql(Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return preparedSql;
    }

    private Object handleAddBatch(Method method, Object[] args) throws Throwable {
        return method.invoke(target, args);
    }

    private Object handleExecuteBatch(Method method, Object[] args) throws Throwable {
        SqlExecutionContext context = SqlExecutionContext.acquire();

        try {
            context.setProxyId(parentProxyId);
            context.setMethodName("executeBatch");
            context.setClassName("Statement");
            context.setSql(preparedSql != null ? preparedSql : "BATCH_EXECUTION");
            context.setThreadName(Thread.currentThread().getName());
            context.setThreadId(Thread.currentThread().getId());
            context.setStartTimeNanos(System.nanoTime());

            Object result = method.invoke(target, args);

            long elapsedNanos = System.nanoTime() - context.getStartTimeNanos();
            if (sqlMonitor != null) {
                sqlMonitor.recordSuccess(context, elapsedNanos, result);
            }

            return result;

        } catch (Throwable t) {
            long elapsedNanos = System.nanoTime() - context.getStartTimeNanos();
            if (sqlMonitor != null) {
                sqlMonitor.recordFailure(context, elapsedNanos, t);
            }
            throw t;
        } finally {
            SqlExecutionContext.release(context);
        }
    }

    private Object wrapResultIfNeeded(Object result, Method method) {
        return result;
    }
}