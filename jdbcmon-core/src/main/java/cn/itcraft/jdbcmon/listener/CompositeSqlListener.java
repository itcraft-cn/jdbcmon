package cn.itcraft.jdbcmon.listener;

import cn.itcraft.jdbcmon.event.MonEvent;

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
    public void onEvent(MonEvent event) {
        for (SqlExecutionListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception ignored) {
            }
        }
    }
}