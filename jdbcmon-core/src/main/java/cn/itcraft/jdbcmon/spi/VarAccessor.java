package cn.itcraft.jdbcmon.spi;

public interface VarAccessor<T> {

    T get();

    void set(T value);

    boolean compareAndSet(T expect, T update);

    static <T> VarAccessor<T> create(Class<T> type, T initialValue) {
        return cn.itcraft.jdbcmon.internal.Platform.createVarAccessor(type, initialValue);
    }
}