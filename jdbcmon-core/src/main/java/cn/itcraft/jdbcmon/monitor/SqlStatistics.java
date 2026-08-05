package cn.itcraft.jdbcmon.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL 监控统计数据快照
 * <p>
 * 从 {@link SqlMonitor} 获取的不可变统计快照，用于外部监控和展示。
 * <p>
 * <h3>数据来源</h3>
 * <ul>
 *   <li>totalQueries: 查询语句执行总数</li>
 *   <li>totalUpdates: 更新语句执行总数</li>
 *   <li>totalBatchOps: 批量操作执行总数</li>
 *   <li>totalErrors: 错误总数</li>
 *   <li>totalSlowQueries: 慢查询总数</li>
 *   <li>slowQueries: TOP N 慢查询列表（按最大耗时排序）</li>
 *   <li>metricsMap: 所有 SQL 的详细指标映射</li>
 * </ul>
 * 
 * @see SqlMonitor#getStatistics()
 */
public final class SqlStatistics {

    private long totalQueries;
    private long totalUpdates;
    private long totalBatchOps;
    private long totalErrors;
    private long totalSlowQueries;
    private long currentSlowQueryThreshold;

    private final List<SlowQueryInfo> slowQueries = new ArrayList<>();
    private Map<String, SqlMetrics> metricsMap;

    public long getTotalQueries() {
        return totalQueries;
    }

    public void setTotalQueries(long totalQueries) {
        this.totalQueries = totalQueries;
    }

    public long getTotalUpdates() {
        return totalUpdates;
    }

    public void setTotalUpdates(long totalUpdates) {
        this.totalUpdates = totalUpdates;
    }

    public long getTotalBatchOps() {
        return totalBatchOps;
    }

    public void setTotalBatchOps(long totalBatchOps) {
        this.totalBatchOps = totalBatchOps;
    }

    public long getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(long totalErrors) {
        this.totalErrors = totalErrors;
    }

    public long getTotalSlowQueries() {
        return totalSlowQueries;
    }

    public void setTotalSlowQueries(long totalSlowQueries) {
        this.totalSlowQueries = totalSlowQueries;
    }

    public long getCurrentSlowQueryThreshold() {
        return currentSlowQueryThreshold;
    }

    public void setCurrentSlowQueryThreshold(long currentSlowQueryThreshold) {
        this.currentSlowQueryThreshold = currentSlowQueryThreshold;
    }

    public List<SlowQueryInfo> getSlowQueries() {
        return slowQueries;
    }

    public void addSlowQuery(String sql, SqlMetrics metrics) {
        slowQueries.add(new SlowQueryInfo(sql, metrics));
    }

    public Map<String, SqlMetrics> getMetricsMap() {
        return metricsMap;
    }

    public void setMetricsMap(Map<String, SqlMetrics> metricsMap) {
        this.metricsMap = metricsMap;
    }

    public long getTotalExecutions() {
        return totalQueries + totalUpdates + totalBatchOps;
    }

    public double getErrorRate() {
        long total = getTotalExecutions();
        return total > 0 ? (double) totalErrors / total : 0;
    }

    public static final class SlowQueryInfo {
        private final String sql;
        private final SqlMetrics metrics;

        public SlowQueryInfo(String sql, SqlMetrics metrics) {
            this.sql = sql;
            this.metrics = metrics;
        }

        public String getSql() {
            return sql;
        }

        public SqlMetrics getMetrics() {
            return metrics;
        }
    }
}