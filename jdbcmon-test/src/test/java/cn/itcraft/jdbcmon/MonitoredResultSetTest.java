package cn.itcraft.jdbcmon;

import cn.itcraft.jdbcmon.config.HugeResultSetAction;
import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.event.HugeResultSetEvent;
import cn.itcraft.jdbcmon.event.MonEvent;
import cn.itcraft.jdbcmon.exception.HugeResultSetException;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.wrap.MonitoredResultSet;
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

@DisplayName("Unit tests for MonitoredResultSet")
class MonitoredResultSetTest {

    private static final AtomicInteger DB_COUNTER = new AtomicInteger(0);

    private DataSource dataSource;
    private SqlMonitor monitor;
    private WrappedConfig config;

    @BeforeEach
    void setUp() throws Exception {
        int dbNum = DB_COUNTER.incrementAndGet();

        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:rs_test_" + dbNum + ";DB_CLOSE_DELAY=-1");
        h2DataSource.setUser("sa");
        h2DataSource.setPassword("");

        config = new WrappedConfig.Builder()
            .hugeResultSetThreshold(10)
            .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
            .build();

        monitor = new SqlMonitor(config);
        dataSource = h2DataSource;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE rs_test (id INT PRIMARY KEY, name VARCHAR(100))");
            for (int i = 0; i < 100; i++) {
                stmt.execute("INSERT INTO rs_test VALUES (" + i + ", 'name" + i + "')");
            }
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS rs_test");
        }
    }

    @Test
    @DisplayName("test_next_countsRows")
    void test_next_countsRows() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rawRs = stmt.executeQuery("SELECT * FROM rs_test WHERE id < 5")) {

            ResultSet rs = new MonitoredResultSet(rawRs, monitor, "SELECT * FROM rs_test",
                config.getHugeResultSetThreshold(), config.getHugeResultSetAction());

            int count = 0;
            while (rs.next()) {
                count++;
            }

            assertEquals(5, count);
        }
    }

    @Test
    @DisplayName("test_close_recordsResultSetSize")
    void test_close_recordsResultSetSize() throws Exception {
        String sql = "SELECT * FROM rs_test WHERE id < 5";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rawRs = stmt.executeQuery(sql)) {

            monitor.getOrCreateMetrics(sql);

            ResultSet rs = new MonitoredResultSet(rawRs, monitor, sql,
                config.getHugeResultSetThreshold(), config.getHugeResultSetAction());

            while (rs.next()) {
            }
            rs.close();
        }

        assertEquals(5, monitor.getOrCreateMetrics(sql).getTotalResultRows());
    }

    @Test
    @DisplayName("test_hugeResultSet_notifyImmediate")
    void test_hugeResultSet_notifyImmediate() throws Exception {
        List<MonEvent> events = new ArrayList<>();
        monitor.addListener(events::add);

        WrappedConfig testConfig = new WrappedConfig.Builder()
            .hugeResultSetThreshold(5)
            .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
            .build();

        SqlMonitor testMonitor = new SqlMonitor(testConfig);
        testMonitor.addListener(events::add);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rawRs = stmt.executeQuery("SELECT * FROM rs_test WHERE id < 20")) {

            ResultSet rs = new MonitoredResultSet(rawRs, testMonitor, "SELECT * FROM rs_test",
                testConfig.getHugeResultSetThreshold(), testConfig.getHugeResultSetAction());

            while (rs.next()) {
            }
        }

        Thread.sleep(100);

        assertFalse(events.isEmpty());
        assertTrue(events.get(0) instanceof HugeResultSetEvent);
        HugeResultSetEvent event = (HugeResultSetEvent) events.get(0);
        assertEquals(5, event.getRowCount());
        assertEquals(5, event.getThreshold());
    }

    @Test
    @DisplayName("test_hugeResultSet_notifyAfter")
    void test_hugeResultSet_notifyAfter() throws Exception {
        List<MonEvent> events = new ArrayList<>();
        monitor.addListener(events::add);

        WrappedConfig testConfig = new WrappedConfig.Builder()
            .hugeResultSetThreshold(5)
            .hugeResultSetAction(HugeResultSetAction.NOTIFY_AFTER)
            .build();

        SqlMonitor testMonitor = new SqlMonitor(testConfig);
        testMonitor.addListener(events::add);

        String sql = "SELECT * FROM rs_test WHERE id < 20";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rawRs = stmt.executeQuery(sql)) {

            testMonitor.getOrCreateMetrics(sql);

            ResultSet rs = new MonitoredResultSet(rawRs, testMonitor, sql,
                testConfig.getHugeResultSetThreshold(), testConfig.getHugeResultSetAction());

            while (rs.next()) {
            }
            rs.close();
        }

        Thread.sleep(100);

        assertFalse(events.isEmpty());
        assertTrue(events.get(0) instanceof HugeResultSetEvent);
        HugeResultSetEvent event = (HugeResultSetEvent) events.get(0);
        assertEquals(20, event.getRowCount());
    }

    @Test
    @DisplayName("test_hugeResultSet_throwException")
    void test_hugeResultSet_throwException() throws Exception {
        WrappedConfig testConfig = new WrappedConfig.Builder()
            .hugeResultSetThreshold(10)
            .hugeResultSetAction(HugeResultSetAction.THROW_EXCEPTION)
            .build();

        SqlMonitor testMonitor = new SqlMonitor(testConfig);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rawRs = stmt.executeQuery("SELECT * FROM rs_test WHERE id < 20")) {

            ResultSet rs = new MonitoredResultSet(rawRs, testMonitor, "SELECT * FROM rs_test",
                testConfig.getHugeResultSetThreshold(), testConfig.getHugeResultSetAction());

            int count = 0;
            while (rs.next()) {
                count++;
            }

            fail("Should have thrown HugeResultSetException at row 10, but read " + count + " rows");
        } catch (HugeResultSetException e) {
            assertEquals(10, e.getRowCount());
            assertEquals(10, e.getThreshold());
        }
    }

    @Test
    @DisplayName("test_threshold_boundary")
    void test_threshold_boundary() throws Exception {
        List<MonEvent> events = new ArrayList<>();
        monitor.addListener(events::add);

        WrappedConfig testConfig = new WrappedConfig.Builder()
            .hugeResultSetThreshold(10)
            .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
            .build();

        SqlMonitor testMonitor = new SqlMonitor(testConfig);
        testMonitor.addListener(events::add);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rawRs = stmt.executeQuery("SELECT * FROM rs_test WHERE id < 10")) {

            ResultSet rs = new MonitoredResultSet(rawRs, testMonitor, "SELECT * FROM rs_test",
                testConfig.getHugeResultSetThreshold(), testConfig.getHugeResultSetAction());

            while (rs.next()) {
            }
        }

        Thread.sleep(100);

        assertFalse(events.isEmpty());
        HugeResultSetEvent event = (HugeResultSetEvent) events.get(0);
        assertEquals(10, event.getRowCount());
    }

    @Test
    @DisplayName("test_getXxx_delegates")
    void test_getXxx_delegates() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rawRs = stmt.executeQuery("SELECT * FROM rs_test WHERE id = 1")) {

            ResultSet rs = new MonitoredResultSet(rawRs, monitor, "SELECT * FROM rs_test",
                config.getHugeResultSetThreshold(), config.getHugeResultSetAction());

            assertTrue(rs.next());
            assertEquals(1, rs.getInt("id"));
            assertEquals("name1", rs.getString("name"));
            assertFalse(rs.next());
        }
    }

    @Test
    @DisplayName("test_isClosed")
    void test_isClosed() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rawRs = stmt.executeQuery("SELECT * FROM rs_test WHERE id = 1")) {

            ResultSet rs = new MonitoredResultSet(rawRs, monitor, "SELECT * FROM rs_test",
                config.getHugeResultSetThreshold(), config.getHugeResultSetAction());

            assertFalse(rs.isClosed());
            rs.close();
            assertTrue(rs.isClosed());
        }
    }

    @Test
    @DisplayName("test_smallResultSet_noEvent")
    void test_smallResultSet_noEvent() throws Exception {
        List<MonEvent> events = new ArrayList<>();
        monitor.addListener(events::add);

        WrappedConfig testConfig = new WrappedConfig.Builder()
            .hugeResultSetThreshold(100)
            .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
            .build();

        SqlMonitor testMonitor = new SqlMonitor(testConfig);
        testMonitor.addListener(events::add);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rawRs = stmt.executeQuery("SELECT * FROM rs_test WHERE id < 5")) {

            ResultSet rs = new MonitoredResultSet(rawRs, testMonitor, "SELECT * FROM rs_test",
                testConfig.getHugeResultSetThreshold(), testConfig.getHugeResultSetAction());

            while (rs.next()) {
            }
        }

        Thread.sleep(50);

        assertTrue(events.isEmpty());
    }
}