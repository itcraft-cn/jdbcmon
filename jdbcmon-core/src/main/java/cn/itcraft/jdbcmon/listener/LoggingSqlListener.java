package cn.itcraft.jdbcmon.listener;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;
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
    public void onSuccess(SqlExecutionContext context, long elapsedNanos, Object result) {
        long elapsedMs = elapsedNanos / 1_000_000;
        String sql = context.getSql();
        if (shouldLog()) {
            log(log, "[JDBCMON] Success: {}ms - {}", elapsedMs, sql);
        }
    }

    @Override
    public void onFailure(SqlExecutionContext context, long elapsedNanos, Throwable throwable) {
        long elapsedMs = elapsedNanos / 1_000_000;
        log.error("[JDBCMON] Failure: {}ms - {} - {}", elapsedMs, context.getSql(), throwable.getMessage());
    }

    @Override
    public void onSlowQuery(SqlExecutionContext context, long elapsedMillis) {
        log.warn("[JDBCMON] SLOW QUERY: {}ms - {}", elapsedMillis, context.getSql());
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