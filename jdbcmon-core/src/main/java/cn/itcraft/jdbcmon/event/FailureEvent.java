package cn.itcraft.jdbcmon.event;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;

public final class FailureEvent extends AbstractMonEvent {

    private final Throwable error;

    public FailureEvent(Object source, SqlExecutionContext context, long elapsedNanos, Throwable error) {
        super(source, context, elapsedNanos);
        this.error = error;
    }

    @Override
    public EventType getEventType() {
        return EventType.FAILURE;
    }

    public Throwable getError() {
        return error;
    }

    public String getErrorMessage() {
        return error != null ? error.getMessage() : null;
    }

    public long getElapsedMillis() {
        return elapsedNanos / 1_000_000;
    }
}