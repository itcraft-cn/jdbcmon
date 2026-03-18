package cn.itcraft.jdbcmon.core;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectionProxyHandler implements InvocationHandler {

    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private final Connection target;
    private final SqlMonitor sqlMonitor;
    private final ProxyConfig config;
    private final long parentProxyId;

    private String currentSql;
    private PreparedStatement currentPreparedStatement;

    public ConnectionProxyHandler(Connection target, SqlMonitor sqlMonitor, ProxyConfig config, long parentProxyId) {
        this.target = target;
        this.sqlMonitor = sqlMonitor;
        this.config = config;
        this.parentProxyId = parentProxyId;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (methodName.equals("close")) {
            currentSql = null;
            currentPreparedStatement = null;
            return method.invoke(target, args);
        }

        if (methodName.equals("prepareStatement") || methodName.equals("prepareCall")) {
            return handlePrepare(method, args);
        }

        if (methodName.equals("createStatement")) {
            return handleCreateStatement(method, args);
        }

        if (methodName.equals("commit") || methodName.equals("rollback")) {
            return handleTransaction(method, args);
        }

        return method.invoke(target, args);
    }

    private Object handlePrepare(Method method, Object[] args) throws Throwable {
        if (args != null && args.length > 0 && args[0] instanceof String) {
            currentSql = (String) args[0];
        }

        Object result = method.invoke(target, args);

        if (result instanceof PreparedStatement) {
            currentPreparedStatement = (PreparedStatement) result;
            return wrapPreparedStatement((PreparedStatement) result, currentSql);
        }

        return result;
    }

    private Object handleCreateStatement(Method method, Object[] args) throws Throwable {
        Object result = method.invoke(target, args);
        if (result instanceof java.sql.Statement) {
            return wrapStatement((java.sql.Statement) result);
        }
        return result;
    }

    private Object handleTransaction(Method method, Object[] args) throws Throwable {
        long startNanos = System.nanoTime();

        try {
            Object result = method.invoke(target, args);

            if (sqlMonitor != null) {
                long elapsedNanos = System.nanoTime() - startNanos;
                SqlExecutionContext context = SqlExecutionContext.acquire();
                try {
                    context.setMethodName(method.getName());
                    context.setSql("TRANSACTION_" + method.getName().toUpperCase());
                    context.setThreadName(Thread.currentThread().getName());
                    context.setThreadId(Thread.currentThread().getId());
                    sqlMonitor.recordSuccess(context, elapsedNanos, result);
                } finally {
                    SqlExecutionContext.release(context);
                }
            }

            return result;
        } catch (Throwable t) {
            if (sqlMonitor != null) {
                long elapsedNanos = System.nanoTime() - startNanos;
                SqlExecutionContext context = SqlExecutionContext.acquire();
                try {
                    context.setMethodName(method.getName());
                    context.setSql("TRANSACTION_" + method.getName().toUpperCase());
                    sqlMonitor.recordFailure(context, elapsedNanos, t);
                } finally {
                    SqlExecutionContext.release(context);
                }
            }
            throw t;
        }
    }

    private PreparedStatement wrapPreparedStatement(PreparedStatement stmt, String sql) {
        return (PreparedStatement) java.lang.reflect.Proxy.newProxyInstance(
            stmt.getClass().getClassLoader(),
            new Class[]{PreparedStatement.class},
            new StatementProxyHandler(stmt, sqlMonitor, config, parentProxyId, sql)
        );
    }

    private java.sql.Statement wrapStatement(java.sql.Statement stmt) {
        return (java.sql.Statement) java.lang.reflect.Proxy.newProxyInstance(
            stmt.getClass().getClassLoader(),
            new Class[]{java.sql.Statement.class},
            new StatementProxyHandler(stmt, sqlMonitor, config, parentProxyId)
        );
    }
}