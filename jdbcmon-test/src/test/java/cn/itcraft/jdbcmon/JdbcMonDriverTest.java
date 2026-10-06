package cn.itcraft.jdbcmon;

import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.driver.DriverUrlParser;
import cn.itcraft.jdbcmon.driver.JdbcMonDriver;
import cn.itcraft.jdbcmon.driver.JdbcMonDriverConfig;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.monitor.SqlStatistics;
import cn.itcraft.jdbcmon.wrap.MonitoredConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Driver 模式（模式2）单元测试
 */
@DisplayName("Unit tests for JdbcMonDriver")
class JdbcMonDriverTest {

    private final JdbcMonDriver driver = new JdbcMonDriver();

    @BeforeEach
    void setUp() {
        JdbcMonDriverConfig.setConfig(new WrappedConfig.Builder()
            .sampleRate(10000)
            .slowQueryThresholdMs(1)
            .build());
    }

    @AfterEach
    void tearDown() {
        JdbcMonDriverConfig.reload();
    }

    @Test
    @DisplayName("test_urlPrefix_constant")
    void test_urlPrefix_constant() {
        assertEquals("jdbc:jdbcmon:", DriverUrlParser.URL_PREFIX);
        assertEquals(DriverUrlParser.URL_PREFIX, JdbcMonDriver.URL_PREFIX);
    }

    @Test
    @DisplayName("test_isMonitoredUrl")
    void test_isMonitoredUrl() {
        assertTrue(DriverUrlParser.isMonitoredUrl("jdbc:jdbcmon:mysql://h:3306/db"));
        assertTrue(DriverUrlParser.isMonitoredUrl("JDBC:JDBCMON:h2:mem:x"));
        assertFalse(DriverUrlParser.isMonitoredUrl("jdbc:mysql://h:3306/db"));
        assertFalse(DriverUrlParser.isMonitoredUrl(null));
    }

    @Test
    @DisplayName("test_toRealUrl")
    void test_toRealUrl() {
        assertEquals("jdbc:mysql://host:3306/db",
            DriverUrlParser.toRealUrl("jdbc:jdbcmon:mysql://host:3306/db"));
        assertEquals("jdbc:h2:mem:test",
            DriverUrlParser.toRealUrl("jdbc:jdbcmon:h2:mem:test"));
        assertEquals("jdbc:oracle:thin:@host:1521:orcl",
            DriverUrlParser.toRealUrl("jdbc:jdbcmon:oracle:thin:@host:1521:orcl"));
    }

    @Test
    @DisplayName("test_toRealUrl_invalid")
    void test_toRealUrl_invalid() {
        assertThrows(IllegalArgumentException.class,
            () -> DriverUrlParser.toRealUrl("jdbc:mysql://host/db"));
    }

    @Test
    @DisplayName("test_acceptsURL")
    void test_acceptsURL() throws Exception {
        assertTrue(driver.acceptsURL("jdbc:jdbcmon:mysql://host/db"));
        assertFalse(driver.acceptsURL("jdbc:mysql://host/db"));
        assertFalse(driver.acceptsURL(null));
    }

    @Test
    @DisplayName("test_connect_nonMonitoredUrl_returnsNull")
    void test_connect_nonMonitoredUrl_returnsNull() throws Exception {
        assertNull(driver.connect("jdbc:mysql://host/db", new Properties()));
    }

    @Test
    @DisplayName("test_connect_returnsMonitoredConnection")
    void test_connect_returnsMonitoredConnection() throws Exception {
        String url = "jdbc:jdbcmon:h2:mem:drv_wrap_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(url)) {
            assertNotNull(conn);
            assertTrue(conn instanceof MonitoredConnection,
                "Connection should be wrapped as MonitoredConnection");
        }
    }

    @Test
    @DisplayName("test_connect_and_monitor")
    void test_connect_and_monitor() throws Exception {
        SqlMonitor monitor = JdbcMonDriverConfig.getMonitor();
        String url = "jdbc:jdbcmon:h2:mem:drv_mon_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE t_drv (id INT PRIMARY KEY)");
            stmt.executeUpdate("INSERT INTO t_drv VALUES (1)");

            try (ResultSet rs = stmt.executeQuery("SELECT * FROM t_drv")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("id"));
            }
        }

        SqlStatistics stats = monitor.getStatistics();
        assertTrue(stats.getTotalExecutions() > 0,
            "Monitoring should record executions, got: " + stats.getTotalExecutions());
    }

    @Test
    @DisplayName("test_driver_metadata")
    void test_driver_metadata() throws Exception {
        assertEquals(1, driver.getMajorVersion());
        assertEquals(0, driver.getMinorVersion());
        assertFalse(driver.jdbcCompliant());
        assertEquals(0, driver.getPropertyInfo(null, null).length);
        assertNotNull(driver.getParentLogger());
    }

    @Test
    @DisplayName("test_config_from_system_property")
    void test_config_from_system_property() {
        System.setProperty("jdbcmon.slowQueryThresholdMs", "2500");
        System.setProperty("jdbcmon.sampleRate", "5000");
        try {
            JdbcMonDriverConfig.reload();
            WrappedConfig cfg = JdbcMonDriverConfig.getConfig();
            assertEquals(2500L, cfg.getSlowQueryThresholdMs());
            assertEquals(5000, cfg.getSampleRate());
        } finally {
            System.clearProperty("jdbcmon.slowQueryThresholdMs");
            System.clearProperty("jdbcmon.sampleRate");
            JdbcMonDriverConfig.reload();
        }
    }

    @Test
    @DisplayName("test_config_invalid_value_falls_back_to_default")
    void test_config_invalid_value_falls_back_to_default() {
        System.setProperty("jdbcmon.sampleRate", "999999");
        try {
            JdbcMonDriverConfig.reload();
            assertNotNull(JdbcMonDriverConfig.getConfig());
            assertEquals(100, JdbcMonDriverConfig.getConfig().getSampleRate(),
                "Invalid sampleRate should fall back to default");
        } finally {
            System.clearProperty("jdbcmon.sampleRate");
            JdbcMonDriverConfig.reload();
        }
    }
}
