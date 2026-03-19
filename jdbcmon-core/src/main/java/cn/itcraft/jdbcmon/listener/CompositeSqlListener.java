package cn.itcraft.jdbcmon.listener;

import cn.itcraft.jdbcmon.core.SqlExecutionContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CompositeSqlListener implements SqlExecutionListener {

    private final List<SqlExecutionListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(SqlExecutionListener listener) {
        if (listener != null && listener != this) {
            listeners.add(listener);
        }
    }

    public void removeListener(SqlExecutionListener listener) {
        listeners.remove(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    public List<SqlExecutionListener> getListeners() {
        return listeners;
    }

    @Override
    public void onSuccess(SqlExecutionContext context, long elapsedNanos, Object result) {
        for (SqlExecutionListener listener : listeners) {
            try {
                listener.onSuccess(context, elapsedNanos, result);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onFailure(SqlExecutionContext context, long elapsedNanos, Throwable throwable) {
        for (SqlExecutionListener listener : listeners) {
            try {
                listener.onFailure(context, elapsedNanos, throwable);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onSlowQuery(SqlExecutionContext context, long elapsedMillis) {
        for (SqlExecutionListener listener : listeners) {
            try {
                listener.onSlowQuery(context, elapsedMillis);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onHugeRetSize(SqlExecutionContext context, int rowCount) {
        for (SqlExecutionListener listener : listeners) {
            try {
                listener.onHugeRetSize(context, rowCount);
            } catch (Exception ignored) {
            }
        }
    }
}