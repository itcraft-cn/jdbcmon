package cn.itcraft.jdbcmon.consts;

/**
 * JDBC 监控常量
 * <p>
 * 定义默认配置值和阈值。
 * <p>
 * <h3>常量分类</h3>
 * <ul>
 *   <li>慢查询阈值：DEFAULT_SLOW_QUERY_THRESHOLD_MS（默认 1000ms）</li>
 *   <li>线程池配置：DEFAULT_CORE_POOL_SIZE、DEFAULT_MAX_POOL_SIZE、DEFAULT_QUEUE_CAPACITY</li>
 *   <li>自适应阈值：ADAPTIVE_WINDOW_SIZE_SECONDS、ADAPTIVE_PERCENTILE</li>
 *   <li>超大结果集：DEFAULT_HUGE_RESULTSET_THRESHOLD（默认 2000 行）</li>
 * </ul>
 */
public final class JdbcConsts {

    public static final long DEFAULT_SLOW_QUERY_THRESHOLD_MS = 1000L;

    public static final int DEFAULT_CORE_POOL_SIZE = 2;
    public static final int DEFAULT_MAX_POOL_SIZE = 4;
    public static final int DEFAULT_QUEUE_CAPACITY = 1000;

    public static final int ADAPTIVE_WINDOW_SIZE_SECONDS = 60;
    public static final double ADAPTIVE_PERCENTILE = 95.0;
    public static final long MIN_ADAPTIVE_THRESHOLD_MS = 100L;
    public static final long MAX_ADAPTIVE_THRESHOLD_MS = 30000L;

    public static final String[] EXECUTE_METHOD_PREFIXES = {"execute", "update", "query", "batch"};

    public static final int DEFAULT_HUGE_RESULTSET_THRESHOLD = 2000;

    private JdbcConsts() {
    }
}