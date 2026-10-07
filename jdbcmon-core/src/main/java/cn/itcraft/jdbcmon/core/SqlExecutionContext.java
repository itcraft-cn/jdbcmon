package cn.itcraft.jdbcmon.core;

import java.util.Objects;

/**
 * SQL 执行上下文
 * <p>
 * 封装单次 SQL 执行的上下文信息，传递给事件监听器。
 * <p>
 * <h3>性能优化</h3>
 * <ul>
 *   <li>ThreadLocal 复用：避免频繁创建对象，减少 GC 压力</li>
 *   <li>acquire/release 模式：显式生命周期管理</li>
 * </ul>
 * 
 * <h3>包含信息</h3>
 * <ul>
 *   <li>代理 ID（proxyId）：标识 Connection 代理实例</li>
 *   <li>SQL 语句、方法名、类名</li>
 *   <li>线程信息（线程名、线程 ID）</li>
 *   <li>批处理信息（批大小、更新计数）</li>
 *   <li>调用栈</li>
 * </ul>
 */
public final class SqlExecutionContext {

    private long proxyId;
    private String methodName;
    private String className;
    private String sql;
    private String threadName;
    private long threadId;
    private long startTimeNanos;
    private String batchId;
    private int batchSize;
    private int[] batchUpdateCounts;
    private StackTraceElement[] stackTrace;

    private static final ThreadLocal<SqlExecutionContext> POOL =
        ThreadLocal.withInitial(SqlExecutionContext::new);

    public static SqlExecutionContext acquire() {
        SqlExecutionContext ctx = POOL.get();
        ctx.reset();
        return ctx;
    }

    public static void release(SqlExecutionContext ctx) {
        if (ctx != null) {
            POOL.set(ctx);
        }
    }

    private void reset() {
        proxyId = 0;
        methodName = null;
        className = null;
        sql = null;
        threadName = null;
        threadId = 0;
        startTimeNanos = 0;
        batchId = null;
        batchSize = 0;
        batchUpdateCounts = null;
        stackTrace = null;
    }

    public long getProxyId() {
        return proxyId;
    }

    public void setProxyId(long proxyId) {
        this.proxyId = proxyId;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public void setStartTimeNanos(long startTimeNanos) {
        this.startTimeNanos = startTimeNanos;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int[] getBatchUpdateCounts() {
        return batchUpdateCounts;
    }

    public void setBatchUpdateCounts(int[] batchUpdateCounts) {
        this.batchUpdateCounts = batchUpdateCounts;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public String toString() {
        return "SqlExecutionContext{" +
            "proxyId=" + proxyId +
            ", methodName='" + methodName + '\'' +
            ", sql='" + sql + '\'' +
            ", threadName='" + threadName + '\'' +
            '}';
    }
}