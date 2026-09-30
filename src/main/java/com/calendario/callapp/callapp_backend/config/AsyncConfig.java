package com.calendario.callapp.callapp_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@Slf4j
/**
 * Configuración del pool de hilos para operaciones asíncronas de notificaciones.
 *
 * <p>Define el bean {@code notificacionesExecutor} que usan los métodos anotados con
 * {@code @Async("notificacionesExecutor")} en {@link com.calendario.callapp.callapp_backend.service.impl.NotificacionServiceImpl}.
 * La política {@link java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy} garantiza
 * que si la cola está llena, el hilo que invocó el método asíncrono lo ejecuta de forma
 * síncrona como fallback, evitando la pérdida de notificaciones.</p>
 */
public class AsyncConfig implements AsyncConfigurer {

    @Value("${app.async.notifications.core-pool-size:3}")
    private int corePoolSize;

    @Value("${app.async.notifications.max-pool-size:6}")
    private int maxPoolSize;

    @Value("${app.async.notifications.queue-capacity:30}")
    private int queueCapacity;

    @Bean(name = "notificacionesExecutor")
    public ThreadPoolTaskExecutor notificacionesExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("notif-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return notificacionesExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("[ASYNC-ERROR] Excepción no capturada en método '{}': {}", method.getName(), ex.getMessage(), ex);
    }
}
