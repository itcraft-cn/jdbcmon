package cn.itcraft.jdbcmon.thread;

import cn.itcraft.jdbcmon.config.WrappedConfig;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步线程执行器
 * <p>
 * 用于异步执行监控事件监听器，避免阻塞业务线程。
 * <p>
 * <h3>线程池配置</h3>
 * <ul>
 *   <li>核心线程数：默认 CPU 核心数 / 2，最小 1</li>
 *   <li>最大线程数：默认 CPU 核心数</li>
 *   <li>队列容量：默认 1000</li>
 *   <li>拒绝策略：CallerRunsPolicy（调用者线程执行）</li>
 * </ul>
 * 
 * <h3>线程特点</h3>
 * <ul>
 *   <li>守护线程：不阻塞 JVM 退出</li>
 *   <li>命名格式：jdbcmon-async-{poolId}-thread-{threadId}</li>
 * </ul>
 */
public final class AsyncThreadExecutor {

    private static final AtomicInteger POOL_COUNTER = new AtomicInteger(0);

    private final ThreadPoolExecutor executor;
    private volatile boolean shutdown = false;

    public AsyncThreadExecutor(WrappedConfig config) {
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

    public void submit(Runnable task) {
        if (shutdown) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }
        executor.submit(task);
    }

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