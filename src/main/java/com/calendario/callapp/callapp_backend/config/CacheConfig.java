package com.calendario.callapp.callapp_backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
/**
 * Configuración del caché en memoria con Caffeine.
 *
 * <p>Registra la caché {@code "userDetails"} con TTL de 45 segundos y capacidad máxima
 * de 2000 entradas. El TTL de 45 s es el tiempo máximo en que el sistema reacciona
 * a la desactivación de un usuario (un usuario desactivado seguirá pudiendo autenticarse
 * hasta que su entrada en caché expire).</p>
 */
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("userDetails");
        // TTL de 45 segundos: un usuario baneado deja de autenticarse en máx 45s.
        // maximumSize=2000: con 500, más de 500 logins distintos en la misma ventana
        // de 45s (plausible en un pico, ej. inicio de semestre) empieza a desalojar
        // entradas (LRU), aumentando consultas a UsuarioRepository justo cuando más
        // se necesita el caché.
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(45, TimeUnit.SECONDS)
                .maximumSize(2000)
                .recordStats());
        return manager;
    }
}
