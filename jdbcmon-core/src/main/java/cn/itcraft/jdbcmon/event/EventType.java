package cn.itcraft.jdbcmon.event;

/**
 * 事件类型枚举
 */
public enum EventType {
    SUCCESS,
    FAILURE,
    SLOW_QUERY,
    HUGE_RESULT_SET
}