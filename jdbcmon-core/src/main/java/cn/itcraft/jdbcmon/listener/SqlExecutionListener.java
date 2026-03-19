package cn.itcraft.jdbcmon.listener;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;

public interface SqlExecutionListener {

    void onSuccess(SqlExecutionContext context, long elapsedNanos, Object result);

    void onFailure(SqlExecutionContext context, long elapsedNanos, Throwable throwable);

    void onSlowQuery(SqlExecutionContext context, long elapsedMillis);

    void onHugeRetSize(SqlExecutionContext context, int rowCount);
}