package cn.itcraft.jdbcmon.event;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;

public final class SlowQueryEvent extends AbstractMonEvent {

    private final long thresholdMs;

    public SlowQueryEvent(Object source, SqlExecutionContext context, long elapsedNanos, long thresholdMs) {
        super(source, context, elapsedNanos);
        this.thresholdMs = thresholdMs;
    }

    @Override
    public EventType getEventType() {
        return EventType.SLOW_QUERY;
    }

    public long getThresholdMs() {
        return thresholdMs;
    }

    public long getElapsedMillis() {
        return elapsedNanos / 1_000_000;
    }

    public StackTraceElement[] getStackTrace() {
        return context != null ? context.getStackTrace() : null;
    }
}