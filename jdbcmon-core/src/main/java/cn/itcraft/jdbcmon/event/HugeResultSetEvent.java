package cn.itcraft.jdbcmon.event;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;

/**
 * 超大结果集事件
 */
public final class HugeResultSetEvent extends AbstractMonEvent {

    private final int rowCount;
    private final int threshold;

    public HugeResultSetEvent(Object source, SqlExecutionContext context, int rowCount, int threshold) {
        super(source, context, 0);
        this.rowCount = rowCount;
        this.threshold = threshold;
    }

    @Override
    public EventType getEventType() {
        return EventType.HUGE_RESULT_SET;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getThreshold() {
        return threshold;
    }
}