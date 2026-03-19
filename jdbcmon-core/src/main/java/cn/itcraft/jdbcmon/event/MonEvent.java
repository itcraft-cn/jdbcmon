package cn.itcraft.jdbcmon.event;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;

public interface MonEvent {

    EventType getEventType();

    SqlExecutionContext getContext();

    long getElapsedNanos();

    long getTimestampMillis();

    Object getSource();

    String getSql();
}