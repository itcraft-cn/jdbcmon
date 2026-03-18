package cn.itcraft.jdbcmon.spi;

import java.util.concurrent.RejectedExecutionException;

public interface AsyncExecutor {

    void submit(Runnable task) throws RejectedExecutionException;

    void shutdown();

    boolean isShutdown();

    static AsyncExecutor create(cn.itcraft.jdbcmon.config.ProxyConfig config) {
        return cn.itcraft.jdbcmon.internal.Platform.createAsyncExecutor(config);
    }
}