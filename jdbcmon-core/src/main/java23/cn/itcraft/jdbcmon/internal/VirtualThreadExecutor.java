package cn.itcraft.jdbcmon.internal;

import cn.itcraft.jdbcmon.spi.AsyncExecutor;

import java.util.concurrent.RejectedExecutionException;

public final class VirtualThreadExecutor implements AsyncExecutor {

    private volatile boolean shutdown = false;

    public VirtualThreadExecutor() {
    }

    @Override
    public void submit(Runnable task) throws RejectedExecutionException {
        if (shutdown) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }

        Thread.ofVirtual().start(task);
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }
}