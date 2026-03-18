package cn.itcraft.jdbcmon.consts;

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

    private JdbcConsts() {
    }
}