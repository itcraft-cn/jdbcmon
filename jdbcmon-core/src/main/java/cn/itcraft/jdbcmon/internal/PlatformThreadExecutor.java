package cn.itcraft.jdbcmon.internal;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.spi.AsyncExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

final class PlatformThreadExecutor implements AsyncExecutor {

    private static final AtomicInteger POOL_COUNTER = new AtomicInteger(0);

    private final ThreadPoolExecutor executor;
    private volatile boolean shutdown = false;

    PlatformThreadExecutor(ProxyConfig config) {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int coreSize = config != null ? config.getCorePoolSize() : Math.max(1, cpuCores / 2);
        int maxSize = config != null ? config.getMaxPoolSize() : cpuCores;
        int queueCapacity = config != null ? config.getQueueCapacity() : 1000;

        this.executor = new ThreadPoolExecutor(
            coreSize,
            maxSize,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            new NamedThreadFactory("jdbcmon-async-" + POOL_COUNTER.incrementAndGet()),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    public void submit(Runnable task) {
        if (shutdown) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }
        executor.submit(task);
    }

    @Override
    public void shutdown() {
        shutdown = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown || executor.isShutdown();
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-thread-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}