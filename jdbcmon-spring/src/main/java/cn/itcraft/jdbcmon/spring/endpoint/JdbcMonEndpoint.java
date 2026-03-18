package cn.itcraft.jdbcmon.spring.endpoint;

import cn.itcraft.jdbcmon.datasource.ProxyDataSource;
import cn.itcraft.jdbcmon.monitor.SqlStatistics;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "jdbcmon")
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
@ConditionalOnBean(ProxyDataSource.class)
public class JdbcMonEndpoint {

    private final ProxyDataSource proxyDataSource;

    public JdbcMonEndpoint(ProxyDataSource proxyDataSource) {
        this.proxyDataSource = proxyDataSource;
    }

    @ReadOperation
    public Map<String, Object> monitor() {
        Map<String, Object> result = new HashMap<>();

        SqlMonitor monitor = proxyDataSource.getSqlMonitor();
        SqlStatistics stats = monitor.getStatistics();

        result.put("totalQueries", stats.getTotalQueries());
        result.put("totalUpdates", stats.getTotalUpdates());
        result.put("totalBatchOps", stats.getTotalBatchOps());
        result.put("totalErrors", stats.getTotalErrors());
        result.put("totalSlowQueries", stats.getTotalSlowQueries());
        result.put("totalExecutions", stats.getTotalExecutions());
        result.put("errorRate", stats.getErrorRate());
        result.put("currentSlowQueryThreshold", stats.getCurrentSlowQueryThreshold());

        return result;
    }
}