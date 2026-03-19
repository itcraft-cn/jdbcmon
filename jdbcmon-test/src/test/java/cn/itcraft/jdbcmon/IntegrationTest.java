package cn.itcraft.jdbcmon;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.wrap.WrappedDataSource;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.monitor.SqlStatistics;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration tests for JDBC monitoring")
class IntegrationTest {

    private DataSource dataSource;
    private WrappedDataSource wrappedDataSource;

    @BeforeEach
    void setUp() {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        h2DataSource.setUser("sa");
        h2DataSource.setPassword("");

        ProxyConfig config = new ProxyConfig.Builder()
            .slowQueryThresholdMs(500)
            .logSlowQueries(true)
            .useAdaptiveThreshold(true)
            .build();

        wrappedDataSource = new WrappedDataSource(h2DataSource, config);
        dataSource = wrappedDataSource;
    }

    @AfterEach
    void tearDown() {
        if (wrappedDataSource != null) {
            wrappedDataSource.shutdown();
        }
    }

    @Test
    @DisplayName("test_executeQuery_recordsMetrics")
    void test_executeQuery_recordsMetrics() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(100))");
            stmt.execute("INSERT INTO test_table VALUES (1, 'test')");

            try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("id"));
                assertEquals("test", rs.getString("name"));
            }
        }

        SqlMonitor monitor = wrappedDataSource.getSqlMonitor();
        SqlStatistics stats = monitor.getStatistics();

        assertTrue(stats.getTotalExecutions() > 0);
        assertEquals(0, stats.getTotalErrors());
    }

    @Test
    @DisplayName("test_executeUpdate_recordsMetrics")
    void test_executeUpdate_recordsMetrics() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE update_test (id INT PRIMARY KEY, value INT)");
            int rows = stmt.executeUpdate("INSERT INTO update_test VALUES (1, 100)");

            assertEquals(1, rows);
        }

        SqlMonitor monitor = wrappedDataSource.getSqlMonitor();
        SqlStatistics stats = monitor.getStatistics();

        assertTrue(stats.getTotalUpdates() > 0);
    }

    @Test
    @DisplayName("test_preparedStatement_recordsMetrics")
    void test_preparedStatement_recordsMetrics() throws Exception {
        try (Connection conn = dataSource.getConnection()) {

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE ps_test (id INT PRIMARY KEY, name VARCHAR(100))");
            }

            try (java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO ps_test VALUES (?, ?)")) {
                ps.setInt(1, 1);
                ps.setString(2, "prepared");
                ps.executeUpdate();
            }

            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT * FROM ps_test WHERE id = ?")) {
                ps.setInt(1, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("prepared", rs.getString("name"));
                }
            }
        }

        SqlMonitor monitor = wrappedDataSource.getSqlMonitor();
        SqlStatistics stats = monitor.getStatistics();

        assertTrue(stats.getTotalExecutions() > 0);
    }

    @Test
    @DisplayName("test_batchExecution_recordsMetrics")
    void test_batchExecution_recordsMetrics() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE batch_test (id INT PRIMARY KEY, value INT)");

            conn.setAutoCommit(false);

            java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO batch_test VALUES (?, ?)");
            for (int i = 1; i <= 10; i++) {
                ps.setInt(1, i);
                ps.setInt(2, i * 10);
                ps.addBatch();
            }

            int[] counts = ps.executeBatch();
            assertEquals(10, counts.length);

            conn.commit();
            ps.close();
        }

        SqlMonitor monitor = wrappedDataSource.getSqlMonitor();
        SqlStatistics stats = monitor.getStatistics();

        assertTrue(stats.getTotalBatchOps() > 0);
    }

    @Test
    @DisplayName("test_connectionProxy_works")
    void test_connectionProxy_works() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
            }
        }
    }

    @Test
    @DisplayName("test_proxyDataSource_isWrapperFor")
    void test_proxyDataSource_isWrapperFor() throws Exception {
        assertTrue(wrappedDataSource.isWrapperFor(WrappedDataSource.class));
        assertTrue(wrappedDataSource.isWrapperFor(DataSource.class));
    }
}