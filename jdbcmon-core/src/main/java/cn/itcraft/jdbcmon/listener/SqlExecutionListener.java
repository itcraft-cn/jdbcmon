package cn.itcraft.jdbcmon.listener;

import cn.itcraft.jdbcmon.event.MonEvent;

/**
 * SQL 执行监听器接口
 */
@FunctionalInterface
public interface SqlExecutionListener {

    void onEvent(MonEvent event);
}