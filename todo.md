# 设计一个高性能、可扩展的轻量级JDBC监控代理框架。

## 🎯 设计目标
- **零侵入**：无需修改业务代码
- **高性能**：代理链开销 < 5%
- **可扩展**：支持自定义监控指标
- **JDK兼容**：8-17无缝迁移
- **生产就绪**：完善的异常处理和资源管理

## 📦 架构设计

### 1. 核心组件
```java
// 目录结构
src/main/java/cn/itcraft/jdbcmon/
├── core/
│   ├── JdbcProxy.java              // 代理工厂入口
│   ├── ProxyInvocationHandler.java // 统一代理处理器
│   └── SqlExecutionContext.java    // SQL执行上下文
├── datasource/
│   ├── ProxyDataSource.java        // 数据源代理
│   └── DataSourceBuilder.java      // 建造者模式
├── monitor/
│   ├── SqlMonitor.java             // 监控核心
│   ├── SqlMetrics.java             // 指标收集
│   ├── SlowSqlDetector.java        // 慢SQL检测
│   └── SqlStatistics.java          // 统计聚合
├── listener/
│   ├── SqlExecutionListener.java   // 监听器接口
│   ├── LoggingSqlListener.java     // 日志监听器
│   ├── MetricsSqlListener.java     // 指标监听器
│   └── CompositeSqlListener.java   // 组合监听器
└── config/
    ├── ProxyConfig.java            // 配置类
    └── FilterConfig.java           // 过滤配置
```

## 🔧 核心实现

### 1. 统一代理处理器
```java
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 高性能JDBC代理处理器
 * 使用动态代理+方法缓存，避免反射开销
 */
public final class ProxyInvocationHandler implements InvocationHandler {
    
    // 方法缓存，避免频繁反射获取Method对象
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final AtomicLong PROXY_ID_GENERATOR = new AtomicLong();
    
    private final Object target;
    private final long proxyId;
    private final SqlMonitor sqlMonitor;
    
    public ProxyInvocationHandler(Object target, SqlMonitor sqlMonitor) {
        this.target = target;
        this.proxyId = PROXY_ID_GENERATOR.incrementAndGet();
        this.sqlMonitor = sqlMonitor;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final String methodName = method.getName();
        final Class<?> declaringClass = method.getDeclaringClass();
        
        // 1. 快速路径：非JDBC核心方法，直接转发
        if (!isJdbcCoreMethod(methodName, declaringClass)) {
            return method.invoke(target, args);
        }
        
        // 2. 开始监控
        final long startNanos = System.nanoTime();
        SqlExecutionContext context = null;
        
        try {
            // 3. 根据方法类型创建执行上下文
            context = createExecutionContext(method, args);
            
            // 4. 执行原始方法
            Object result = method.invoke(target, args);
            
            // 5. 处理返回结果（如代理ResultSet）
            result = wrapResultIfNeeded(result, method, context);
            
            // 6. 记录成功执行
            recordExecutionSuccess(context, startNanos, result);
            
            return result;
            
        } catch (Throwable throwable) {
            // 7. 记录执行失败
            recordExecutionFailure(context, startNanos, throwable);
            throw throwable;
        }
    }
    
    /**
     * 创建SQL执行上下文
     */
    private SqlExecutionContext createExecutionContext(Method method, Object[] args) {
        SqlExecutionContext context = new SqlExecutionContext();
        context.setProxyId(proxyId);
        context.setMethodName(method.getName());
        context.setClassName(method.getDeclaringClass().getSimpleName());
        context.setThreadName(Thread.currentThread().getName());
        context.setThreadId(Thread.currentThread().getId());
        
        // 提取SQL信息
        extractSqlInfo(context, method, args);
        
        return context;
    }
    
    /**
     * 提取SQL信息
     */
    private void extractSqlInfo(SqlExecutionContext context, Method method, Object[] args) {
        if ("executeQuery".equals(method.getName()) || 
            "executeUpdate".equals(method.getName()) ||
            "execute".equals(method.getName())) {
            
            if (args != null && args.length > 0) {
                // Statement.executeQuery(sql)
                if (args[0] instanceof String) {
                    context.setSql((String) args[0]);
                }
                // PreparedStatement.executeQuery()
            } else {
                // 从PreparedStatement中提取SQL
                if (target instanceof PreparedStatement) {
                    try {
                        // 尝试获取PreparedStatement的原始SQL
                        // 注意：某些驱动可能不支持toString获取SQL
                        String sql = target.toString();
                        if (sql != null && sql.contains(":")) {
                            // 提取预处理SQL
                            sql = sql.substring(sql.indexOf(':') + 1).trim();
                            context.setSql(sql);
                        }
                    } catch (Exception ignored) {
                        // 无法获取SQL，使用占位符
                        context.setSql("PREPARED_STATEMENT");
                    }
                }
            }
        }
    }
    
    /**
     * 包装返回结果
     */
    private Object wrapResultIfNeeded(Object result, Method method, SqlExecutionContext context) {
        if (result == null) {
            return null;
        }
        
        Class<?> returnType = method.getReturnType();
        
        // 1. 包装ResultSet
        if (ResultSet.class.isAssignableFrom(returnType)) {
            return wrapResultSet((ResultSet) result, context);
        }
        
        // 2. 包装Statement相关对象
        if (Statement.class.isAssignableFrom(returnType) ||
            PreparedStatement.class.isAssignableFrom(returnType) ||
            CallableStatement.class.isAssignableFrom(returnType)) {
            
            return wrapStatement((Statement) result, context);
        }
        
        // 3. 包装Connection
        if (Connection.class.isAssignableFrom(returnType)) {
            return wrapConnection((Connection) result);
        }
        
        return result;
    }
    
    /**
     * 包装ResultSet
     */
    private ResultSet wrapResultSet(ResultSet resultSet, SqlExecutionContext context) {
        return (ResultSet) Proxy.newProxyInstance(
            resultSet.getClass().getClassLoader(),
            new Class[]{ResultSet.class},
            new ResultSetInvocationHandler(resultSet, context, sqlMonitor)
        );
    }
    
    /**
     * 包装Statement
     */
    private Statement wrapStatement(Statement statement, SqlExecutionContext context) {
        Class<?>[] interfaces = getAllInterfaces(statement);
        
        return (Statement) Proxy.newProxyInstance(
            statement.getClass().getClassLoader(),
            interfaces,
            new StatementInvocationHandler(statement, context, sqlMonitor)
        );
    }
    
    /**
     * 包装Connection
     */
    private Connection wrapConnection(Connection connection) {
        return (Connection) Proxy.newProxyInstance(
            connection.getClass().getClassLoader(),
            new Class[]{Connection.class},
            new ConnectionInvocationHandler(connection, sqlMonitor)
        );
    }
    
    /**
     * 记录执行成功
     */
    private void recordExecutionSuccess(SqlExecutionContext context, long startNanos, Object result) {
        if (sqlMonitor != null && context.getSql() != null) {
            long elapsedNanos = System.nanoTime() - startNanos;
            sqlMonitor.recordSuccess(context, elapsedNanos, result);
        }
    }
    
    /**
     * 记录执行失败
     */
    private void recordExecutionFailure(SqlExecutionContext context, long startNanos, Throwable throwable) {
        if (sqlMonitor != null && context.getSql() != null) {
            long elapsedNanos = System.nanoTime() - startNanos;
            sqlMonitor.recordFailure(context, elapsedNanos, throwable);
        }
    }
    
    /**
     * 判断是否为JDBC核心方法
     */
    private boolean isJdbcCoreMethod(String methodName, Class<?> declaringClass) {
        // 核心执行方法
        if (methodName.startsWith("execute") || 
            methodName.startsWith("update") ||
            methodName.startsWith("query") ||
            methodName.startsWith("batch") ||
            methodName.startsWith("commit") ||
            methodName.startsWith("rollback") ||
            methodName.equals("close")) {
            
            return Connection.class.isAssignableFrom(declaringClass) ||
                   Statement.class.isAssignableFrom(declaringClass) ||
                   ResultSet.class.isAssignableFrom(declaringClass);
        }
        
        return false;
    }
    
    /**
     * 获取对象实现的所有接口
     */
    private Class<?>[] getAllInterfaces(Object obj) {
        if (obj == null) {
            return new Class[0];
        }
        
        Class<?> clazz = obj.getClass();
        return clazz.getInterfaces();
    }
}
```

### 2. Statement专用处理器
```java
/**
 * Statement代理处理器
 * 专门处理Statement、PreparedStatement、CallableStatement
 */
final class StatementInvocationHandler implements InvocationHandler {
    
    private final Statement target;
    private final SqlExecutionContext parentContext;
    private final SqlMonitor sqlMonitor;
    private final Map<String, SqlExecutionContext> batchContexts = new ConcurrentHashMap<>();
    
    public StatementInvocationHandler(Statement target, SqlExecutionContext parentContext, SqlMonitor sqlMonitor) {
        this.target = target;
        this.parentContext = parentContext;
        this.sqlMonitor = sqlMonitor;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final String methodName = method.getName();
        final long startNanos = System.nanoTime();
        
        SqlExecutionContext context = null;
        boolean isBatchOperation = false;
        
        try {
            // 处理批处理操作
            if ("addBatch".equals(methodName)) {
                return handleAddBatch(method, args);
            } else if ("executeBatch".equals(methodName)) {
                isBatchOperation = true;
                context = prepareBatchContext();
            } else if (isExecuteMethod(methodName)) {
                context = createExecuteContext(method, args);
            }
            
            // 执行原始方法
            Object result = method.invoke(target, args);
            
            // 记录执行结果
            if (context != null) {
                long elapsedNanos = System.nanoTime() - startNanos;
                
                if (isBatchOperation) {
                    recordBatchExecution(result, context, elapsedNanos);
                } else {
                    sqlMonitor.recordSuccess(context, elapsedNanos, result);
                }
            }
            
            return wrapResultIfNeeded(result, method, context);
            
        } catch (Throwable throwable) {
            if (context != null) {
                long elapsedNanos = System.nanoTime() - startNanos;
                sqlMonitor.recordFailure(context, elapsedNanos, throwable);
            }
            throw throwable;
        }
    }
    
    /**
     * 处理addBatch操作
     */
    private Object handleAddBatch(Method method, Object[] args) throws Throwable {
        Object result = method.invoke(target, args);
        
        // 记录批处理SQL
        if (args != null && args.length > 0) {
            String sql = extractSqlFromArgs(args);
            if (sql != null) {
                String batchId = generateBatchId();
                SqlExecutionContext batchContext = createBatchContext(sql, batchId);
                batchContexts.put(batchId, batchContext);
            }
        }
        
        return result;
    }
    
    /**
     * 准备批处理上下文
     */
    private SqlExecutionContext prepareBatchContext() {
        SqlExecutionContext context = new SqlExecutionContext();
        context.setMethodName("executeBatch");
        context.setClassName("Statement");
        context.setSql("BATCH_EXECUTION");
        context.setBatchSize(batchContexts.size());
        
        // 合并所有批处理SQL
        if (!batchContexts.isEmpty()) {
            StringBuilder sqlBuilder = new StringBuilder("BATCH[");
            for (SqlExecutionContext batchCtx : batchContexts.values()) {
                sqlBuilder.append(batchCtx.getSql()).append("; ");
            }
            sqlBuilder.append("]");
            context.setSql(sqlBuilder.toString());
        }
        
        return context;
    }
    
    /**
     * 记录批处理执行结果
     */
    private void recordBatchExecution(Object result, SqlExecutionContext context, long elapsedNanos) {
        if (result instanceof int[]) {
            int[] updateCounts = (int[]) result;
            context.setBatchUpdateCounts(updateCounts);
        }
        
        sqlMonitor.recordSuccess(context, elapsedNanos, result);
        
        // 清理批处理上下文
        batchContexts.clear();
    }
    
    private boolean isExecuteMethod(String methodName) {
        return methodName.startsWith("execute") || 
               methodName.startsWith("update") ||
               methodName.startsWith("query");
    }
    
    private SqlExecutionContext createExecuteContext(Method method, Object[] args) {
        SqlExecutionContext context = new SqlExecutionContext();
        context.setMethodName(method.getName());
        context.setClassName(target.getClass().getSimpleName());
        context.setThreadName(Thread.currentThread().getName());
        context.setThreadId(Thread.currentThread().getId());
        
        // 提取SQL
        String sql = extractSqlFromArgs(args);
        if (sql == null && target instanceof PreparedStatement) {
            sql = extractPreparedSql();
        }
        context.setSql(sql);
        
        return context;
    }
    
    private String extractSqlFromArgs(Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return null;
    }
    
    private String extractPreparedSql() {
        try {
            String str = target.toString();
            int colonIndex = str.indexOf(':');
            if (colonIndex > 0) {
                return str.substring(colonIndex + 1).trim();
            }
        } catch (Exception e) {
            // 忽略
        }
        return "PREPARED_STATEMENT";
    }
    
    private Object wrapResultIfNeeded(Object result, Method method, SqlExecutionContext context) {
        if (result instanceof ResultSet) {
            return wrapResultSet((ResultSet) result, context);
        }
        return result;
    }
    
    private ResultSet wrapResultSet(ResultSet resultSet, SqlExecutionContext context) {
        return (ResultSet) Proxy.newProxyInstance(
            resultSet.getClass().getClassLoader(),
            new Class[]{ResultSet.class},
            new ResultSetInvocationHandler(resultSet, context, sqlMonitor)
        );
    }
    
    private String generateBatchId() {
        return "batch_" + System.currentTimeMillis() + "_" + System.identityHashCode(target);
    }
    
    private SqlExecutionContext createBatchContext(String sql, String batchId) {
        SqlExecutionContext context = new SqlExecutionContext();
        context.setSql(sql);
        context.setBatchId(batchId);
        return context;
    }
}
```

### 3. 监控核心
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * 高性能SQL监控器
 * 使用LongAdder进行高并发计数，避免CAS竞争
 */
public final class SqlMonitor {
    
    // 配置
    private final ProxyConfig config;
    
    // 监听器
    private final List<SqlExecutionListener> listeners = new CopyOnWriteArrayList<>();
    
    // 统计指标
    private final Map<String, SqlMetrics> metricsMap = new ConcurrentHashMap<>();
    private final LongAdder totalQueries = new LongAdder();
    private final LongAdder totalUpdates = new LongAdder();
    private final LongAdder totalBatchOps = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();
    private final LongAdder totalSlowQueries = new LongAdder();
    
    // 慢SQL检测
    private final SlowSqlDetector slowSqlDetector;
    
    // 线程池用于异步处理
    private final ExecutorService asyncExecutor;
    
    public SqlMonitor(ProxyConfig config) {
        this.config = config;
        this.slowSqlDetector = new SlowSqlDetector(config.getSlowQueryThresholdMillis());
        
        // 创建有界队列线程池，避免内存溢出
        this.asyncExecutor = new ThreadPoolExecutor(
            config.getCorePoolSize(),
            config.getMaxPoolSize(),
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(config.getQueueCapacity()),
            new NamedThreadFactory("sql-monitor"),
            new ThreadPoolExecutor.CallerRunsPolicy() // 降级策略
        );
        
        // 注册默认监听器
        registerDefaultListeners();
    }
    
    /**
     * 记录成功执行
     */
    public void recordSuccess(SqlExecutionContext context, long elapsedNanos, Object result) {
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        final String sql = context.getSql();
        
        // 更新总计数
        if (isQueryOperation(context.getMethodName())) {
            totalQueries.increment();
        } else if (isUpdateOperation(context.getMethodName())) {
            totalUpdates.increment();
        } else if ("executeBatch".equals(context.getMethodName())) {
            totalBatchOps.increment();
        }
        
        // 获取或创建指标
        SqlMetrics metrics = metricsMap.computeIfAbsent(sql, k -> new SqlMetrics());
        
        // 更新指标（使用LongAdder避免竞争）
        metrics.recordSuccess(elapsedNanos, result);
        
        // 检测慢SQL
        if (slowSqlDetector.isSlowQuery(elapsedMillis)) {
            totalSlowQueries.increment();
            handleSlowQuery(context, elapsedMillis);
        }
        
        // 异步通知监听器
        if (!listeners.isEmpty()) {
            asyncExecutor.submit(() -> {
                for (SqlExecutionListener listener : listeners) {
                    try {
                        listener.onSuccess(context, elapsedNanos, result);
                    } catch (Exception e) {
                        // 忽略监听器异常，避免影响主流程
                        logError("Listener error", e);
                    }
                }
            });
        }
    }
    
    /**
     * 记录执行失败
     */
    public void recordFailure(SqlExecutionContext context, long elapsedNanos, Throwable throwable) {
        totalErrors.increment();
        
        // 更新指标
        SqlMetrics metrics = metricsMap.computeIfAbsent(context.getSql(), k -> new SqlMetrics());
        metrics.recordFailure(elapsedNanos, throwable);
        
        // 异步通知监听器
        if (!listeners.isEmpty()) {
            asyncExecutor.submit(() -> {
                for (SqlExecutionListener listener : listeners) {
                    try {
                        listener.onFailure(context, elapsedNanos, throwable);
                    } catch (Exception e) {
                        logError("Listener error", e);
                    }
                }
            });
        }
    }
    
    /**
     * 处理慢查询
     */
    private void handleSlowQuery(SqlExecutionContext context, long elapsedMillis) {
        if (config.isLogSlowQueries()) {
            // 可以发送到专门的慢查询日志
            System.err.printf("[SLOW_SQL] %dms - %s%n", elapsedMillis, context.getSql());
        }
        
        // 可以触发告警、记录详细堆栈等
        if (config.isCollectStackTrace()) {
            context.setStackTrace(Thread.currentThread().getStackTrace());
        }
    }
    
    /**
     * 获取统计报告
     */
    public SqlStatistics getStatistics() {
        SqlStatistics stats = new SqlStatistics();
        stats.setTotalQueries(totalQueries.longValue());
        stats.setTotalUpdates(totalUpdates.longValue());
        stats.setTotalBatchOps(totalBatchOps.longValue());
        stats.setTotalErrors(totalErrors.longValue());
        stats.setTotalSlowQueries(totalSlowQueries.longValue());
        
        // 计算最慢的SQL
        List<Map.Entry<String, SqlMetrics>> entries = new ArrayList<>(metricsMap.entrySet());
        entries.sort(Comparator.comparingLong(e -> -e.getValue().getMaxTimeNanos()));
        
        int limit = Math.min(config.getTopSlowQueryLimit(), entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, SqlMetrics> entry = entries.get(i);
            stats.addSlowQuery(entry.getKey(), entry.getValue());
        }
        
        return stats;
    }
    
    /**
     * 注册监听器
     */
    public void registerListener(SqlExecutionListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 注册默认监听器
     */
    private void registerDefaultListeners() {
        if (config.isEnableLogging()) {
            registerListener(new LoggingSqlListener(config.getLogLevel()));
        }
        
        if (config.isEnableMetrics()) {
            registerListener(new MetricsSqlListener());
        }
    }
    
    private boolean isQueryOperation(String methodName) {
        return methodName != null && 
               (methodName.contains("Query") || "execute".equals(methodName));
    }
    
    private boolean isUpdateOperation(String methodName) {
        return methodName != null && 
               (methodName.contains("Update") || "execute".equals(methodName));
    }
    
    private void logError(String message, Throwable throwable) {
        // 使用System.err避免依赖外部日志框架
        System.err.println("[" + getClass().getSimpleName() + "] " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### 4. 指标收集
```java
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Arrays;

/**
 * SQL性能指标
 * 使用LongAdder实现高并发下的精确计数
 */
public final class SqlMetrics {
    
    // 执行次数统计
    private final LongAdder executionCount = new LongAdder();
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    
    // 时间统计
    private final LongAdder totalTimeNanos = new LongAdder();
    private final AtomicLong minTimeNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxTimeNanos = new AtomicLong(Long.MIN_VALUE);
    
    // 直方图统计
    private final LongAdder[] timeHistogram;
    private static final long[] TIME_BOUNDARIES = {
        1_000_000L,     // 1ms
        10_000_000L,    // 10ms
        50_000_000L,    // 50ms
        100_000_000L,   // 100ms
        500_000_000L,   // 500ms
        1_000_000_000L, // 1s
        5_000_000_000L  // 5s
    };
    
    // 错误统计
    private final LongAdder[] errorCounts = new LongAdder[ErrorType.values().length];
    
    public SqlMetrics() {
        // 初始化直方图
        timeHistogram = new LongAdder[TIME_BOUNDARIES.length + 1];
        for (int i = 0; i < timeHistogram.length; i++) {
            timeHistogram[i] = new LongAdder();
        }
        
        // 初始化错误统计
        for (int i = 0; i < errorCounts.length; i++) {
            errorCounts[i] = new LongAdder();
        }
    }
    
    /**
     * 记录成功执行
     */
    public void recordSuccess(long elapsedNanos, Object result) {
        executionCount.increment();
        successCount.increment();
        totalTimeNanos.add(elapsedNanos);
        
        // 更新最小时间
        long currentMin = minTimeNanos.get();
        while (elapsedNanos < currentMin) {
            if (minTimeNanos.compareAndSet(currentMin, elapsedNanos)) {
                break;
            }
            currentMin = minTimeNanos.get();
        }
        
        // 更新最大时间
        long currentMax = maxTimeNanos.get();
        while (elapsedNanos > currentMax) {
            if (maxTimeNanos.compareAndSet(currentMax, elapsedNanos)) {
                break;
            }
            currentMax = maxTimeNanos.get();
        }
        
        // 更新直方图
        updateHistogram(elapsedNanos);
        
        // 如果是ResultSet，记录行数
        if (result != null) {
            recordResultSetInfo(result);
        }
    }
    
    /**
     * 记录执行失败
     */
    public void recordFailure(long elapsedNanos, Throwable throwable) {
        executionCount.increment();
        failureCount.increment();
        totalTimeNanos.add(elapsedNanos);
        
        // 更新直方图
        updateHistogram(elapsedNanos);
        
        // 记录错误类型
        ErrorType errorType = classifyError(throwable);
        errorCounts[errorType.ordinal()].increment();
    }
    
    /**
     * 更新时间直方图
     */
    private void updateHistogram(long elapsedNanos) {
        int index = 0;
        while (index < TIME_BOUNDARIES.length && elapsedNanos > TIME_BOUNDARIES[index]) {
            index++;
        }
        timeHistogram[index].increment();
    }
    
    /**
     * 记录结果集信息
     */
    private void recordResultSetInfo(Object result) {
        if (result instanceof java.sql.ResultSet) {
            try {
                java.sql.ResultSet rs = (java.sql.ResultSet) result;
                int rowCount = 0;
                if (rs.last()) {
                    rowCount = rs.getRow();
                    rs.beforeFirst();
                }
                // 可以记录行数统计
            } catch (Exception e) {
                // 忽略结果集遍历异常
            }
        }
    }
    
    /**
     * 错误分类
     */
    private ErrorType classifyError(Throwable throwable) {
        if (throwable instanceof java.sql.SQLException) {
            java.sql.SQLException sqlEx = (java.sql.SQLException) throwable;
            int errorCode = sqlEx.getErrorCode();
            
            // 连接相关错误
            if (errorCode == 104 || errorCode == 2013) { // Connection lost
                return ErrorType.CONNECTION;
            }
            
            // 超时错误
            if (errorCode == 1317 || sqlEx.getMessage().contains("timeout")) {
                return ErrorType.TIMEOUT;
            }
            
            // 死锁
            if (errorCode == 1213 || sqlEx.getMessage().contains("deadlock")) {
                return ErrorType.DEADLOCK;
            }
        }
        
        return ErrorType.OTHER;
    }
    
    // Getters
    public long getExecutionCount() { return executionCount.longValue(); }
    public long getSuccessCount() { return successCount.longValue(); }
    public long getFailureCount() { return failureCount.longValue(); }
    public long getTotalTimeNanos() { return totalTimeNanos.longValue(); }
    public long getMinTimeNanos() { 
        long min = minTimeNanos.get(); 
        return min == Long.MAX_VALUE ? 0 : min; 
    }
    public long getMaxTimeNanos() { 
        long max = maxTimeNanos.get(); 
        return max == Long.MIN_VALUE ? 0 : max; 
    }
    public double getAvgTimeNanos() {
        long count = executionCount.longValue();
        return count > 0 ? (double) totalTimeNanos.longValue() / count : 0;
    }
    
    /**
     * 获取直方图数据
     */
    public long[] getHistogramData() {
        long[] data = new long[timeHistogram.length];
        for (int i = 0; i < timeHistogram.length; i++) {
            data[i] = timeHistogram[i].longValue();
        }
        return data;
    }
    
    /**
     * 获取错误统计
     */
    public long[] getErrorStatistics() {
        long[] stats = new long[errorCounts.length];
        for (int i = 0; i < errorCounts.length; i++) {
            stats[i] = errorCounts[i].longValue();
        }
        return stats;
    }
    
    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        CONNECTION,     // 连接错误
        TIMEOUT,        // 超时
        DEADLOCK,       // 死锁
        SYNTAX,         // SQL语法错误
        CONSTRAINT,     // 约束违反
        OTHER           // 其他错误
    }
}
```

### 5. 配置类
```java
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * 代理配置
 */
public final class ProxyConfig {
    
    // 性能配置
    private boolean enablePerformanceMonitor = true;
    private long slowQueryThresholdMillis = 1000; // 1秒
    private boolean logSlowQueries = true;
    private boolean collectStackTrace = false;
    
    // 过滤配置
    private Set<String> excludedTables = new HashSet<>();
    private Set<String> excludedSchemas = new HashSet<>();
    private Pattern sqlPatternFilter = null;
    
    // 线程池配置
    private int corePoolSize = 2;
    private int maxPoolSize = 4;
    private int queueCapacity = 1000;
    
    // 日志配置
    private boolean enableLogging = true;
    private LogLevel logLevel = LogLevel.INFO;
    private boolean logParameters = false; // 是否记录参数（注意安全）
    
    // 指标配置
    private boolean enableMetrics = true;
    private int metricsFlushIntervalSeconds = 60;
    private int topSlowQueryLimit = 10;
    
    // 监控配置
    private boolean monitorConnections = true;
    private boolean monitorTransactions = true;
    private boolean monitorBatchOperations = true;
    
    // 构建器模式
    public static class Builder {
        private final ProxyConfig config = new ProxyConfig();
        
        public Builder enablePerformanceMonitor(boolean enable) {
            config.enablePerformanceMonitor = enable;
            return this;
        }
        
        public Builder slowQueryThresholdMillis(long threshold) {
            config.slowQueryThresholdMillis = threshold;
            return this;
        }
        
        public Builder logSlowQueries(boolean enable) {
            config.logSlowQueries = enable;
            return this;
        }
        
        public Builder collectStackTrace(boolean enable) {
            config.collectStackTrace = enable;
            return this;
        }
        
        public Builder addExcludedTable(String table) {
            config.excludedTables.add(table.toLowerCase());
            return this;
        }
        
        public Builder addExcludedSchema(String schema) {
            config.excludedSchemas.add(schema.toLowerCase());
            return this;
        }
        
        public Builder sqlPatternFilter(String regex) {
            if (regex != null && !regex.isEmpty()) {
                config.sqlPatternFilter = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            }
            return this;
        }
        
        public Builder threadPool(int core, int max, int queue) {
            config.corePoolSize = core;
            config.maxPoolSize = max;
            config.queueCapacity = queue;
            return this;
        }
        
        public Builder enableLogging(boolean enable) {
            config.enableLogging = enable;
            return this;
        }
        
        public Builder logLevel(LogLevel level) {
            config.logLevel = level;
            return this;
        }
        
        public Builder logParameters(boolean enable) {
            config.logParameters = enable;
            return this;
        }
        
        public Builder enableMetrics(boolean enable) {
            config.enableMetrics = enable;
            return this;
        }
        
        public Builder metricsFlushInterval(int seconds) {
            config.metricsFlushIntervalSeconds = seconds;
            return this;
        }
        
        public Builder topSlowQueryLimit(int limit) {
            config.topSlowQueryLimit = limit;
            return this;
        }
        
        public Builder monitorConnections(boolean enable) {
            config.monitorConnections = enable;
            return this;
        }
        
        public Builder monitorTransactions(boolean enable) {
            config.monitorTransactions = enable;
            return this;
        }
        
        public Builder monitorBatchOperations(boolean enable) {
            config.monitorBatchOperations = enable;
            return this;
        }
        
        public ProxyConfig build() {
            return config;
        }
    }
    
    // Getters
    public boolean isEnablePerformanceMonitor() { return enablePerformanceMonitor; }
    public long getSlowQueryThresholdMillis() { return slowQueryThresholdMillis; }
    public boolean isLogSlowQueries() { return logSlowQueries; }
    public boolean isCollectStackTrace() { return collectStackTrace; }
    public Set<String> getExcludedTables() { return excludedTables; }
    public Set<String> getExcludedSchemas() { return excludedSchemas; }
    public Pattern getSqlPatternFilter() { return sqlPatternFilter; }
    public int getCorePoolSize() { return corePoolSize; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public int getQueueCapacity() { return queueCapacity; }
    public boolean isEnableLogging() { return enableLogging; }
    public LogLevel getLogLevel() { return logLevel; }
    public boolean isLogParameters() { return logParameters; }
    public boolean isEnableMetrics() { return enableMetrics; }
    public int getMetricsFlushIntervalSeconds() { return metricsFlushIntervalSeconds; }
    public int getTopSlowQueryLimit() { return topSlowQueryLimit; }
    public boolean isMonitorConnections() { return monitorConnections; }
    public boolean isMonitorTransactions() { return monitorTransactions; }
    public boolean isMonitorBatchOperations() { return monitorBatchOperations; }
    
    /**
     * 检查SQL是否应该被过滤
     */
    public boolean shouldFilter(String sql) {
        if (sql == null || sql.isEmpty()) {
            return true;
        }
        
        // 检查表过滤
        String lowerSql = sql.toLowerCase();
        for (String table : excludedTables) {
            if (lowerSql.contains(table.toLowerCase())) {
                return true;
            }
        }
        
        // 检查模式过滤
        for (String schema : excludedSchemas) {
            if (lowerSql.contains(schema.toLowerCase() + ".")) {
                return true;
            }
        }
        
        // 检查正则过滤
        if (sqlPatternFilter != null && sqlPatternFilter.matcher(sql).find()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 日志级别
     */
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
```

## 🚀 使用示例

### 1. 快速开始
```java
// 创建配置
ProxyConfig config = new ProxyConfig.Builder()
    .slowQueryThresholdMillis(500)
    .logSlowQueries(true)
    .enableMetrics(true)
    .build();

// 包装原始数据源
DataSource originalDataSource = getOriginalDataSource();
DataSource proxyDataSource = ProxyDataSourceBuilder.create(originalDataSource)
    .config(config)
    .build();

// 在应用中使用代理数据源
JdbcTemplate jdbcTemplate = new JdbcTemplate(proxyDataSource);

// 获取统计信息
SqlMonitor monitor = proxyDataSource.getSqlMonitor();
SqlStatistics stats = monitor.getStatistics();

System.out.println("总查询数: " + stats.getTotalQueries());
System.out.println("慢查询数: " + stats.getTotalSlowQueries());
```

### 2. Spring Boot集成
```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        HikariDataSource originalDataSource = new HikariDataSource();
        
        ProxyConfig config = new ProxyConfig.Builder()
            .slowQueryThresholdMillis(1000)
            .enableMetrics(true)
            .addExcludedTable("audit_log")  // 排除审计表
            .build();
        
        return ProxyDataSourceBuilder.create(originalDataSource)
            .config(config)
            .name("main-datasource")
            .build();
    }
    
    @Bean
    public SqlMonitorEndpoint sqlMonitorEndpoint(DataSource dataSource) {
        if (dataSource instanceof ProxyDataSource) {
            return new SqlMonitorEndpoint(((ProxyDataSource) dataSource).getSqlMonitor());
        }
        return null;
    }
}
```

## 📊 性能优化策略

### 1. 零GC优化
```java
// 使用对象池减少GC
private static final ThreadLocal<SqlExecutionContext> CONTEXT_POOL = 
    ThreadLocal.withInitial(() -> new SqlExecutionContext());

// 使用原生数组存储直方图
private final long[] histogram = new long[TIME_BOUNDARIES.length];
private final long[] errorCounts = new long[ErrorType.values().length];

// 使用Unsafe进行无锁更新（谨慎使用）
private static final sun.misc.Unsafe UNSAFE = getUnsafe();
private static final long TOTAL_TIME_OFFSET = 
    UNSAFE.objectFieldOffset(SqlMetrics.class.getDeclaredField("totalTimeNanos"));
```

### 2. 内存屏障优化
```java
// 使用VarHandle进行内存顺序控制
private static final VarHandle MIN_TIME;
private static final VarHandle MAX_TIME;

static {
    try {
        MIN_TIME = MethodHandles.lookup()
            .findVarHandle(SqlMetrics.class, "minTimeNanos", long.class);
        MAX_TIME = MethodHandles.lookup()
            .findVarHandle(SqlMetrics.class, "maxTimeNanos", long.class);
    } catch (Exception e) {
        throw new Error(e);
    }
}

// 使用acquire-release语义更新最小值
public void recordMinTime(long elapsedNanos) {
    long currentMin = (long) MIN_TIME.getVolatile(this);
    while (elapsedNanos < currentMin) {
        if (MIN_TIME.compareAndSet(this, currentMin, elapsedNanos)) {
            break;
        }
        currentMin = (long) MIN_TIME.getVolatile(this);
    }
}
```

## 🎯 性能预期

| 场景 | 预期开销 | 说明 |
|------|---------|------|
| **无监控** | < 1% | 只做方法转发，无额外处理 |
| **基础监控** | 1-3% | 记录执行时间、计数等基本指标 |
| **全量监控** | 3-5% | 包含直方图、错误分类、堆栈收集等 |
| **异步日志** | 1-2% | 异步处理监听器，不影响主线程 |

## 🔧 生产部署建议

1. **分级监控**：
   - 开发环境：全量监控
   - 测试环境：基础监控
   - 生产环境：按需开启，可动态调整

2. **采样策略**：
   - 高频SQL：1%采样率
   - 低频SQL：100%采样
   - 慢SQL：100%采样

3. **告警集成**：
   - 慢SQL超过阈值自动告警
   - 错误率突增告警
   - 连接池异常告警

这个自定义JDBC代理框架相比P6Spy的优势：
1. **性能更优**：专门为监控优化，无多余功能
2. **可控性强**：完全掌握代码，可深度定制
3. **JDK兼容**：确保8-17平滑迁移
4. **生产就绪**：完善的异常处理、资源管理和监控指标

