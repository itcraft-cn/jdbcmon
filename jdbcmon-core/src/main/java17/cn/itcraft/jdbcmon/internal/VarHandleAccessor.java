package cn.itcraft.jdbcmon.internal;

import cn.itcraft.jdbcmon.spi.VarAccessor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

public final class VarHandleAccessor<T> implements VarAccessor<T> {

    private static final VarHandle VALUE_HANDLE;

    static {
        try {
            VALUE_HANDLE = MethodHandles.lookup()
                .findVarHandle(VarHandleAccessor.class, "value", Object.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused")
    private volatile T value;

    public VarHandleAccessor(Class<T> type, T initialValue) {
        this.value = Objects.requireNonNull(initialValue, "initialValue cannot be null");
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        return (T) VALUE_HANDLE.getVolatile(this);
    }

    @Override
    public void set(T value) {
        VALUE_HANDLE.setVolatile(this, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean compareAndSet(T expect, T update) {
        return VALUE_HANDLE.compareAndSet(this, expect, update);
    }
}