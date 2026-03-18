package cn.itcraft.jdbcmon.core;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.spi.MethodInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ProxyInvocationHandler implements java.lang.reflect.InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(ProxyInvocationHandler.class);

    private static final Map<String, MethodInvoker> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final AtomicLong PROXY_ID_GENERATOR = new AtomicLong();

    private final Object target;
    private final long proxyId;
    private final SqlMonitor sqlMonitor;
    private final ProxyConfig config;

    public ProxyInvocationHandler(Object target, SqlMonitor sqlMonitor, ProxyConfig config) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.sqlMonitor = sqlMonitor;
        this.config = config != null ? config : new ProxyConfig.Builder().build();
        this.proxyId = PROXY_ID_GENERATOR.incrementAndGet();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?> declaringClass = method.getDeclaringClass();

        if (isObjectMethod(methodName, declaringClass)) {
            return handleObjectMethod(methodName, args);
        }

        if (methodName.equals("close")) {
            return handleClose(args);
        }

        if (!isJdbcCoreMethod(methodName, declaringClass)) {
            return invokeMethod(method, args);
        }

        return handleJdbcMethod(method, args);
    }

    private boolean isObjectMethod(String methodName, Class<?> declaringClass) {
        return declaringClass == Object.class;
    }

    private Object handleObjectMethod(String methodName, Object[] args) {
        switch (methodName) {
            case "hashCode":
                return System.identityHashCode(target);
            case "equals":
                return args != null && args.length > 0 && args[0] == this;
            case "toString":
                return "JdbcProxy[" + target.getClass().getSimpleName() + "#" + proxyId + "]";
            default:
                return null;
        }
    }

    private Object handleClose(Object[] args) throws Throwable {
        return invokeMethod(getCloseMethod(), args);
    }

    private Method getCloseMethod() throws NoSuchMethodException {
        return target.getClass().getMethod("close");
    }

    private boolean isJdbcCoreMethod(String methodName, Class<?> declaringClass) {
        if (methodName.startsWith("execute") ||
            methodName.startsWith("update") ||
            methodName.startsWith("query") ||
            methodName.startsWith("batch") ||
            methodName.startsWith("commit") ||
            methodName.startsWith("rollback")) {

            return Connection.class.isAssignableFrom(declaringClass) ||
                   Statement.class.isAssignableFrom(declaringClass) ||
                   ResultSet.class.isAssignableFrom(declaringClass) ||
                   PreparedStatement.class.isAssignableFrom(declaringClass);
        }
        return false;
    }

    private Object handleJdbcMethod(Method method, Object[] args) throws Throwable {
        SqlExecutionContext context = SqlExecutionContext.acquire();

        try {
            context.setProxyId(proxyId);
            context.setMethodName(method.getName());
            context.setClassName(method.getDeclaringClass().getSimpleName());
            context.setThreadName(Thread.currentThread().getName());
            context.setThreadId(Thread.currentThread().getId());
            context.setStartTimeNanos(System.nanoTime());

            extractSqlInfo(context, method, args);

            Object result = invokeMethod(method, args);

            result = wrapResultIfNeeded(result, method);

            recordSuccess(context, result);

            return result;

        } catch (Throwable throwable) {
            recordFailure(context, throwable);
            throw throwable;
        } finally {
            SqlExecutionContext.release(context);
        }
    }

    private void extractSqlInfo(SqlExecutionContext context, Method method, Object[] args) {
        String methodName = method.getName();

        if (args != null && args.length > 0 && args[0] instanceof String) {
            context.setSql((String) args[0]);
            return;
        }

        if (target instanceof PreparedStatement) {
            String sql = extractPreparedSql();
            if (sql != null) {
                context.setSql(sql);
            }
        }
    }

    private String extractPreparedSql() {
        try {
            String str = target.toString();
            int colonIndex = str.indexOf(':');
            if (colonIndex > 0 && colonIndex < str.length() - 1) {
                return str.substring(colonIndex + 1).trim();
            }
        } catch (Exception e) {
            log.debug("Failed to extract prepared SQL: {}", e.getMessage());
        }
        return null;
    }

    private Object invokeMethod(Method method, Object[] args) throws Throwable {
        String cacheKey = method.getDeclaringClass().getName() + "#" + method.getName();
        MethodInvoker invoker = METHOD_CACHE.computeIfAbsent(cacheKey, k -> MethodInvoker.create(method));
        return invoker.invoke(target, args);
    }

    private Object wrapResultIfNeeded(Object result, Method method) {
        if (result == null) {
            return null;
        }

        Class<?> returnType = method.getReturnType();

        if (ResultSet.class.isAssignableFrom(returnType)) {
            return wrapResultSet((ResultSet) result);
        }

        if (Statement.class.isAssignableFrom(returnType) && !(result instanceof PreparedStatement)) {
            return wrapStatement((Statement) result);
        }

        if (Connection.class.isAssignableFrom(returnType)) {
            return wrapConnection((Connection) result);
        }

        return result;
    }

    private ResultSet wrapResultSet(ResultSet resultSet) {
        return (ResultSet) java.lang.reflect.Proxy.newProxyInstance(
            resultSet.getClass().getClassLoader(),
            new Class[]{ResultSet.class},
            new ResultSetProxyHandler(resultSet, sqlMonitor, config, proxyId)
        );
    }

    private Statement wrapStatement(Statement statement) {
        Class<?>[] interfaces = statement.getClass().getInterfaces();
        if (interfaces.length == 0) {
            interfaces = new Class[]{Statement.class};
        }

        return (Statement) java.lang.reflect.Proxy.newProxyInstance(
            statement.getClass().getClassLoader(),
            interfaces,
            new StatementProxyHandler(statement, sqlMonitor, config, proxyId)
        );
    }

    private Connection wrapConnection(Connection connection) {
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
            connection.getClass().getClassLoader(),
            new Class[]{Connection.class},
            new ConnectionProxyHandler(connection, sqlMonitor, config, proxyId)
        );
    }

    private void recordSuccess(SqlExecutionContext context, Object result) {
        if (sqlMonitor != null && context.getSql() != null) {
            long elapsedNanos = System.nanoTime() - context.getStartTimeNanos();
            sqlMonitor.recordSuccess(context, elapsedNanos, result);
        }
    }

    private void recordFailure(SqlExecutionContext context, Throwable throwable) {
        if (sqlMonitor != null) {
            long elapsedNanos = System.nanoTime() - context.getStartTimeNanos();
            sqlMonitor.recordFailure(context, elapsedNanos, throwable);
        }
    }

    public Object getTarget() {
        return target;
    }

    public long getProxyId() {
        return proxyId;
    }
}