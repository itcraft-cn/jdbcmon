package cn.itcraft.jdbcmon.event;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;

abstract class AbstractMonEvent implements MonEvent {

    protected final Object source;
    protected final SqlExecutionContext context;
    protected final long elapsedNanos;
    protected final long timestampMillis;

    AbstractMonEvent(Object source, SqlExecutionContext context, long elapsedNanos) {
        this.source = source;
        this.context = context;
        this.elapsedNanos = elapsedNanos;
        this.timestampMillis = System.currentTimeMillis();
    }

    @Override
    public EventType getEventType() {
        return null;
    }

    @Override
    public SqlExecutionContext getContext() {
        return context;
    }

    @Override
    public long getElapsedNanos() {
        return elapsedNanos;
    }

    @Override
    public long getTimestampMillis() {
        return timestampMillis;
    }

    @Override
    public Object getSource() {
        return source;
    }

    @Override
    public String getSql() {
        return context != null ? context.getSql() : null;
    }
}