package cn.itcraft.jdbcmon.internal;

import cn.itcraft.jdbcmon.spi.VarAccessor;

import java.util.concurrent.atomic.AtomicReference;

final class AtomicReferenceAccessor<T> implements VarAccessor<T> {

    private final AtomicReference<T> ref;

    AtomicReferenceAccessor(T initialValue) {
        this.ref = new AtomicReference<>(initialValue);
    }

    @Override
    public T get() {
        return ref.get();
    }

    @Override
    public void set(T value) {
        ref.set(value);
    }

    @Override
    public boolean compareAndSet(T expect, T update) {
        return ref.compareAndSet(expect, update);
    }
}