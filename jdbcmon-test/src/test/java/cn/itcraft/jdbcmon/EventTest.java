package cn.itcraft.jdbcmon;

import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.core.SqlExecutionContext;
import cn.itcraft.jdbcmon.event.*;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests for Event system")
class EventTest {

    private SqlMonitor monitor;
    private SqlExecutionContext context;

    @BeforeEach
    void setUp() {
        WrappedConfig config = new WrappedConfig.Builder().build();
        monitor = new SqlMonitor(config);
        context = new SqlExecutionContext();
        context.setSql("SELECT * FROM test");
        context.setMethodName("executeQuery");
    }

    @Test
    @DisplayName("test_successEvent_properties")
    void test_successEvent_properties() {
        long elapsedNanos = 1_000_000L;
        Object result = 42;

        SuccessEvent event = new SuccessEvent(monitor, context, elapsedNanos, result);

        assertEquals(EventType.SUCCESS, event.getEventType());
        assertEquals(elapsedNanos, event.getElapsedNanos());
        assertEquals(1L, event.getElapsedMillis());
        assertEquals(result, event.getResult());
        assertEquals(42, event.getResultAsInt());
        assertEquals(context, event.getContext());
        assertEquals("SELECT * FROM test", event.getSql());
        assertEquals(monitor, event.getSource());
        assertTrue(event.getTimestampMillis() > 0);
    }

    @Test
    @DisplayName("test_successEvent_resultAsInt_withNull")
    void test_successEvent_resultAsInt_withNull() {
        SuccessEvent event = new SuccessEvent(monitor, context, 1_000_000L, null);

        assertEquals(0, event.getResultAsInt());
    }

    @Test
    @DisplayName("test_successEvent_resultAsInt_withLong")
    void test_successEvent_resultAsInt_withLong() {
        SuccessEvent event = new SuccessEvent(monitor, context, 1_000_000L, 123456789L);

        assertEquals(123456789, event.getResultAsInt());
    }

    @Test
    @DisplayName("test_failureEvent_properties")
    void test_failureEvent_properties() {
        long elapsedNanos = 2_000_000L;
        RuntimeException error = new RuntimeException("Connection failed");

        FailureEvent event = new FailureEvent(monitor, context, elapsedNanos, error);

        assertEquals(EventType.FAILURE, event.getEventType());
        assertEquals(elapsedNanos, event.getElapsedNanos());
        assertEquals(2L, event.getElapsedMillis());
        assertEquals(error, event.getError());
        assertEquals("Connection failed", event.getErrorMessage());
        assertEquals(context, event.getContext());
        assertEquals(monitor, event.getSource());
    }

    @Test
    @DisplayName("test_failureEvent_errorMessage_withNull")
    void test_failureEvent_errorMessage_withNull() {
        FailureEvent event = new FailureEvent(monitor, context, 1_000_000L, null);

        assertNull(event.getErrorMessage());
    }

    @Test
    @DisplayName("test_slowQueryEvent_properties")
    void test_slowQueryEvent_properties() {
        long elapsedNanos = 5_000_000_000L;
        long thresholdMs = 1000L;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        context.setStackTrace(stackTrace);

        SlowQueryEvent event = new SlowQueryEvent(monitor, context, elapsedNanos, thresholdMs);

        assertEquals(EventType.SLOW_QUERY, event.getEventType());
        assertEquals(elapsedNanos, event.getElapsedNanos());
        assertEquals(5000L, event.getElapsedMillis());
        assertEquals(thresholdMs, event.getThresholdMs());
        assertEquals(stackTrace, event.getStackTrace());
        assertEquals(context, event.getContext());
        assertEquals(monitor, event.getSource());
    }

    @Test
    @DisplayName("test_slowQueryEvent_stackTrace_withNullContext")
    void test_slowQueryEvent_stackTrace_withNullContext() {
        SlowQueryEvent event = new SlowQueryEvent(monitor, null, 1_000_000L, 100L);

        assertNull(event.getStackTrace());
        assertNull(event.getSql());
    }

    @Test
    @DisplayName("test_hugeResultSetEvent_properties")
    void test_hugeResultSetEvent_properties() {
        int rowCount = 5000;
        int threshold = 2000;

        HugeResultSetEvent event = new HugeResultSetEvent(monitor, context, rowCount, threshold);

        assertEquals(EventType.HUGE_RESULT_SET, event.getEventType());
        assertEquals(rowCount, event.getRowCount());
        assertEquals(threshold, event.getThreshold());
        assertEquals(context, event.getContext());
        assertEquals("SELECT * FROM test", event.getSql());
        assertEquals(monitor, event.getSource());
        assertTrue(event.getTimestampMillis() > 0);
    }

    @Test
    @DisplayName("test_eventType_enum")
    void test_eventType_enum() {
        EventType[] types = EventType.values();

        assertEquals(4, types.length);
        assertEquals(EventType.SUCCESS, EventType.valueOf("SUCCESS"));
        assertEquals(EventType.FAILURE, EventType.valueOf("FAILURE"));
        assertEquals(EventType.SLOW_QUERY, EventType.valueOf("SLOW_QUERY"));
        assertEquals(EventType.HUGE_RESULT_SET, EventType.valueOf("HUGE_RESULT_SET"));
    }

    @Test
    @DisplayName("test_monEvent_interface")
    void test_monEvent_interface() {
        MonEvent event = new SuccessEvent(monitor, context, 1_000_000L, null);

        assertNotNull(event.getEventType());
        assertNotNull(event.getContext());
        assertTrue(event.getElapsedNanos() >= 0);
        assertTrue(event.getTimestampMillis() > 0);
        assertNotNull(event.getSource());
    }
}