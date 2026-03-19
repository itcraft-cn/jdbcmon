package cn.itcraft.jdbcmon.event;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;

public final class SuccessEvent extends AbstractMonEvent {

    private final Object result;

    public SuccessEvent(Object source, SqlExecutionContext context, long elapsedNanos, Object result) {
        super(source, context, elapsedNanos);
        this.result = result;
    }

    @Override
    public EventType getEventType() {
        return EventType.SUCCESS;
    }

    public Object getResult() {
        return result;
    }

    public int getResultAsInt() {
        if (result instanceof Integer) {
            return (Integer) result;
        }
        if (result instanceof Number) {
            return ((Number) result).intValue();
        }
        return 0;
    }

    public long getElapsedMillis() {
        return elapsedNanos / 1_000_000;
    }
}