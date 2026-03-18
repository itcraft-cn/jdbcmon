package cn.itcraft.jdbcmon.internal;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;

public final class ScopedValueContext {

    private static final ScopedValue<SqlExecutionContext> CURRENT_CONTEXT = ScopedValue.newInstance();

    public static ScopedValue<SqlExecutionContext> context() {
        return CURRENT_CONTEXT;
    }

    public static SqlExecutionContext get() {
        return CURRENT_CONTEXT.orElse(null);
    }

    public static boolean isBound() {
        return CURRENT_CONTEXT.isBound();
    }

    public static void withContext(SqlExecutionContext context, Runnable action) {
        ScopedValue.where(CURRENT_CONTEXT, context).run(action);
    }
}