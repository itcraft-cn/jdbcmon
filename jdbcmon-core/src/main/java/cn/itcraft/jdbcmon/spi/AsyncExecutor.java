package cn.itcraft.jdbcmon.spi;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.internal.PlatformThreadExecutor;

import java.util.concurrent.RejectedExecutionException;

public interface AsyncExecutor {

    void submit(Runnable task) throws RejectedExecutionException;

    void shutdown();

    boolean isShutdown();

    static AsyncExecutor create(ProxyConfig config) {
        return new PlatformThreadExecutor(config);
    }
}