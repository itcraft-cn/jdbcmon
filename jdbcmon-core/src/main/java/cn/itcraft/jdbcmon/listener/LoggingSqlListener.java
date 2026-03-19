package cn.itcraft.jdbcmon.listener;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;
import cn.itcraft.jdbcmon.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingSqlListener implements SqlExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingSqlListener.class);

    private final LogLevel logLevel;

    public LoggingSqlListener() {
        this(LogLevel.INFO);
    }

    public LoggingSqlListener(LogLevel logLevel) {
        this.logLevel = logLevel != null ? logLevel : LogLevel.INFO;
    }

    @Override
    public void onEvent(MonEvent event) {
        switch (event.getEventType()) {
            case SUCCESS:
                logSuccess((SuccessEvent) event);
                break;
            case FAILURE:
                logFailure((FailureEvent) event);
                break;
            case SLOW_QUERY:
                logSlowQuery((SlowQueryEvent) event);
                break;
            case HUGE_RESULT_SET:
                logHugeResultSet((HugeResultSetEvent) event);
                break;
            default:
                break;
        }
    }

    private void logSuccess(SuccessEvent event) {
        if (!shouldLog()) {
            return;
        }
        SqlExecutionContext ctx = event.getContext();
        long elapsedMs = event.getElapsedMillis();
        log(log, "[JDBCMON] Success: {}ms - {}", elapsedMs, ctx.getSql());
    }

    private void logFailure(FailureEvent event) {
        SqlExecutionContext ctx = event.getContext();
        long elapsedMs = event.getElapsedMillis();
        log.error("[JDBCMON] Failure: {}ms - {} - {}", elapsedMs, ctx.getSql(), event.getErrorMessage());
    }

    private void logSlowQuery(SlowQueryEvent event) {
        SqlExecutionContext ctx = event.getContext();
        log.warn("[JDBCMON] SLOW QUERY: {}ms (threshold: {}ms) - {}",
            event.getElapsedMillis(), event.getThresholdMs(), ctx.getSql());
    }

    private void logHugeResultSet(HugeResultSetEvent event) {
        SqlExecutionContext ctx = event.getContext();
        log.warn("[JDBCMON] HUGE RESULTSET: {} rows (threshold: {}) - {}",
            event.getRowCount(), event.getThreshold(), ctx.getSql());
    }

    private boolean shouldLog() {
        switch (logLevel) {
            case DEBUG:
                return log.isDebugEnabled();
            case INFO:
                return log.isInfoEnabled();
            case WARN:
                return log.isWarnEnabled();
            case ERROR:
                return log.isErrorEnabled();
            default:
                return log.isInfoEnabled();
        }
    }

    private void log(Logger logger, String format, Object... args) {
        switch (logLevel) {
            case DEBUG:
                logger.debug(format, args);
                break;
            case INFO:
                logger.info(format, args);
                break;
            case WARN:
                logger.warn(format, args);
                break;
            case ERROR:
                logger.error(format, args);
                break;
        }
    }

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}