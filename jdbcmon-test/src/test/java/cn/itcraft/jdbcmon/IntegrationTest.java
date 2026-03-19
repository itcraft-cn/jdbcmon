package cn.itcraft.jdbcmon;

import cn.itcraft.jdbcmon.config.HugeResultSetAction;
import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.event.HugeResultSetEvent;
import cn.itcraft.jdbcmon.event.MonEvent;
import cn.itcraft.jdbcmon.event.SuccessEvent;
import cn.itcraft.jdbcmon.exception.HugeResultSetException;
import cn.itcraft.jdbcmon.wrap.MonitoredResultSet;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration tests for JDBC monitoring")
class IntegrationTest {

    private static final AtomicInteger DB_COUNTER = new AtomicInteger(0);

    private DataSource dataSource;
    private WrappedDataSource wrappedDataSource;

    @BeforeEach
    void setUp() {
        int dbNum = DB_COUNTER.incrementAndGet();

        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:testdb_" + dbNum + ";DB_CLOSE_DELAY=-1");
        h2DataSource.setUser("sa");
        h2DataSource.setPassword("");

        WrappedConfig config = new WrappedConfig.Builder()
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

            stmt.execute("CREATE TABLE update_test (id INT PRIMARY KEY, val INT)");
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

            stmt.execute("CREATE TABLE batch_test (id INT PRIMARY KEY, val INT)");

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

    @Test
    @DisplayName("test_resultSet_wrapped")
    void test_resultSet_wrapped() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE rs_test (id INT PRIMARY KEY, name VARCHAR(100))");
            for (int i = 0; i < 10; i++) {
                stmt.execute("INSERT INTO rs_test VALUES (" + i + ", 'name" + i + "')");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT * FROM rs_test")) {
                assertTrue(rs instanceof MonitoredResultSet);
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                assertEquals(10, count);
            }
        }
    }

    @Test
    @DisplayName("test_hugeResultSetDetection_triggersEvent")
    void test_hugeResultSetDetection_triggersEvent() throws Exception {
        List<MonEvent> events = new ArrayList<>();
        wrappedDataSource.getSqlMonitor().addListener(events::add);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE huge_test (id INT PRIMARY KEY)");
            for (int i = 0; i < 3000; i++) {
                stmt.execute("INSERT INTO huge_test VALUES (" + i + ")");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT * FROM huge_test")) {
                while (rs.next()) {
                }
            }
        }

        Thread.sleep(100);

        boolean foundHugeEvent = events.stream()
            .anyMatch(e -> e instanceof HugeResultSetEvent);

        assertTrue(foundHugeEvent, "Should have received HugeResultSetEvent");
    }

    @Test
    @DisplayName("test_eventListener_receivesEvents")
    void test_eventListener_receivesEvents() throws Exception {
        List<MonEvent> events = new ArrayList<>();
        
        WrappedConfig eventConfig = new WrappedConfig.Builder()
            .enableLogging(false)
            .hugeResultSetThreshold(5)
            .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
            .build();
        
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:event_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        h2DataSource.setUser("sa");
        h2DataSource.setPassword("");
        
        WrappedDataSource eventDataSource = new WrappedDataSource(h2DataSource, eventConfig);
        eventDataSource.getSqlMonitor().addListener(events::add);

        try (Connection conn = eventDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE event_test (id INT PRIMARY KEY)");
            for (int i = 0; i < 100; i++) {
                stmt.execute("INSERT INTO event_test VALUES (" + i + ")");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT * FROM event_test WHERE id < 20")) {
                while (rs.next()) {
                }
            }
        }

        Thread.sleep(300);

        eventDataSource.shutdown();

        assertTrue(events.size() >= 1, "Should have received at least 1 event, got: " + events.size());
        
        boolean foundHugeEvent = events.stream()
            .anyMatch(e -> e instanceof HugeResultSetEvent);
        assertTrue(foundHugeEvent, "Should have at least 1 HugeResultSetEvent");
    }

    @Test
    @DisplayName("test_hugeResultSetException_thrown")
    void test_hugeResultSetException_thrown() throws Exception {
        WrappedConfig throwConfig = new WrappedConfig.Builder()
            .hugeResultSetThreshold(100)
            .hugeResultSetAction(HugeResultSetAction.THROW_EXCEPTION)
            .build();

        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:throw_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        h2DataSource.setUser("sa");
        h2DataSource.setPassword("");

        WrappedDataSource throwDataSource = new WrappedDataSource(h2DataSource, throwConfig);
        try (Connection conn = throwDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE throw_test (id INT PRIMARY KEY)");
            for (int i = 0; i < 200; i++) {
                stmt.execute("INSERT INTO throw_test VALUES (" + i + ")");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT * FROM throw_test")) {
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                fail("Should have thrown HugeResultSetException, but read " + count + " rows");
            }
        } catch (HugeResultSetException e) {
            assertEquals(100, e.getRowCount());
            assertEquals(100, e.getThreshold());
        } finally {
            throwDataSource.shutdown();
        }
    }
}