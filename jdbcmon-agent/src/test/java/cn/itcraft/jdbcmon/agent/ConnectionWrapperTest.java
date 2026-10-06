package cn.itcraft.jdbcmon.agent;

import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.wrap.MonitoredConnection;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConnectionWrapper 单元测试
 */
class ConnectionWrapperTest {

    @BeforeEach
    void setUp() {
        JdbcMonAgentConfig.setConfig(new WrappedConfig.Builder()
            .sampleRate(10000)
            .slowQueryThresholdMs(1)
            .build());
    }

    @AfterEach
    void tearDown() {
        JdbcMonAgentConfig.reload();
    }

    @Test
    void wrap_null_returnsNull() {
        assertNull(ConnectionWrapper.wrap(null));
    }

    @Test
    void wrap_rawConnection_returnsMonitoredConnection() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:cw_wrap_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");

        try (Connection raw = ds.getConnection()) {
            assertFalse(raw instanceof MonitoredConnection);
            Connection wrapped = ConnectionWrapper.wrap(raw);
            assertTrue(wrapped instanceof MonitoredConnection);
        }
    }

    @Test
    void wrap_idempotent() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:cw_idem_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");

        try (Connection raw = ds.getConnection()) {
            Connection first = ConnectionWrapper.wrap(raw);
            Connection second = ConnectionWrapper.wrap(first);
            assertSame(first, second, "Wrapping an already monitored connection must be a no-op");
        }
    }

    @Test
    void wrappedConnection_recordsMetrics() throws Exception {
        SqlMonitor monitor = JdbcMonAgentConfig.getMonitor();
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:cw_metrics_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");

        try (Connection raw = ds.getConnection()) {
            Connection wrapped = ConnectionWrapper.wrap(raw);
            try (Statement stmt = wrapped.createStatement()) {
                stmt.execute("CREATE TABLE t_cw (id INT PRIMARY KEY)");
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM t_cw")) {
                    assertFalse(rs.next());
                }
            }
        }

        assertTrue(monitor.getStatistics().getTotalExecutions() > 0);
    }
}
