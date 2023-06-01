package org.rpc.utils.concurrent.threadpool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;


import java.util.concurrent.*;
import java.util.Map;


@Slf4j
public class ThreadPoolFactoryUntil {
    private static final Map<String, ExecutorService> THREAD_POOLS = new ConcurrentHashMap<>();
    private ThreadPoolFactoryUntil() {

    }

    public static ExecutorService createCustomThreadPoolIfAbsent ( String threadNamePrefix ) {
        CustomThreadPoolConfig customThreadPoolConfig = new CustomThreadPoolConfig();
        return createCustomThreadPoolIfAbsent(customThreadPoolConfig, threadNamePrefix, false);
    }

    public static ExecutorService createCustomThreadPoolIfAbsent ( String threadNamePrefix, CustomThreadPoolConfig customThreadPoolConfig ) {
        return createCustomThreadPoolIfAbsent(customThreadPoolConfig, threadNamePrefix, false);
    }

    public static ExecutorService createCustomThreadPoolIfAbsent ( CustomThreadPoolConfig customThreadPoolConfig, String threadNamePrefix, Boolean daemon ) {
        ExecutorService threadPool = THREAD_POOLS.computeIfAbsent(threadNamePrefix, k -> createThreadPool(customThreadPoolConfig, threadNamePrefix, daemon));
        if ( threadPool.isShutdown() || threadPool.isTerminated() ) {
            THREAD_POOLS.remove(threadNamePrefix);
            threadPool = createThreadPool(customThreadPoolConfig, threadNamePrefix, false);
            THREAD_POOLS.put( threadNamePrefix, threadPool );
        }

        return threadPool;
    }

    private static ExecutorService createThreadPool( CustomThreadPoolConfig customThreadPoolConfig, String threadNamePrefix, Boolean daemon ) {
        ThreadFactory threadFactory = createThreadFactory( threadNamePrefix, daemon );
        return new ThreadPoolExecutor(customThreadPoolConfig.getCorePoolSize(), customThreadPoolConfig.getMaximumPoolSize(), customThreadPoolConfig.getKeepAliveTime(), customThreadPoolConfig.getUnit(), customThreadPoolConfig.getWorkQueue(), threadFactory);
    }

    public static ThreadFactory createThreadFactory( String threadNamePrefix, Boolean daemon ) {
        if ( threadNamePrefix != null ) {
            if ( daemon != null ) {
                return new ThreadFactoryBuilder().setNameFormat(threadNamePrefix + "-%d").setDaemon(daemon).build();
            } else {
                return new ThreadFactoryBuilder().setNameFormat(threadNamePrefix + "-%d").build();
            }
        }

        return Executors.defaultThreadFactory();
    }

    public static void shutDownAllThreadPool() {
        log.info("call shutDownAllThreadPool method");
        THREAD_POOLS.entrySet().parallelStream().forEach(entry -> {
            ExecutorService executorService = entry.getValue();
            executorService.shutdown();
            log.info("shut down thread pool [{}] [{}]", entry.getKey(), executorService.isTerminated());

            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Thread pool never terminated");
                executorService.shutdownNow();
            }
        });
    }

    public static void printThreadPoolStatus( ThreadPoolExecutor threadPoolExecutor ) {
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, createThreadFactory("print-thread-pool-status", false));
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            log.info("============ThreadPool Status=============");
            log.info("ThreadPool Size: [{}]", threadPoolExecutor.getPoolSize());
            log.info("Active Threads: [{}]", threadPoolExecutor.getActiveCount());
            log.info("Number of Tasks : [{}]", threadPoolExecutor.getCompletedTaskCount());
            log.info("Number of Tasks in Queue: {}", threadPoolExecutor.getQueue().size());
            log.info("===========================================");
        }, 0, 1, TimeUnit.SECONDS);
    }
}
