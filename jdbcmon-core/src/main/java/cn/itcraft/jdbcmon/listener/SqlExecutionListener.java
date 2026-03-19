package cn.itcraft.jdbcmon.listener;

import cn.itcraft.jdbcmon.event.MonEvent;

@FunctionalInterface
public interface SqlExecutionListener {

    void onEvent(MonEvent event);
}