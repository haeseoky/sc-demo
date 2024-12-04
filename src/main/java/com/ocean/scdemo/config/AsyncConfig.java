package com.ocean.scdemo.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    private static final String ASYNC_THREAD_PREFIX = "virtual-thread-";

    @Bean
    public Executor virtualTaskExecutor() {

        return new VirtualThreadTaskExecutor(ASYNC_THREAD_PREFIX);
    }

    @Bean
    public Executor normalTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("normal-thread-");
//        executor.initialize();
        return executor;

    }
}
