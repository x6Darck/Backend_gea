package com.calendario.callapp.callapp_backend.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Publicar un evento importante ya no debe bloquear el hilo/transacción de
 * {@code publicar()} esperando la respuesta de Firebase: sendMulticastNotification
 * debe ejecutarse en el pool async de notificaciones, no en el hilo llamador.
 */
@SpringBootTest
@ActiveProfiles("test")
class PushNotificationServiceAsyncTest {

    @Autowired
    private PushNotificationService pushNotificationService;

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(PushNotificationService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void sendMulticastNotification_corre_en_el_pool_async_no_en_el_hilo_llamador() throws InterruptedException {
        String hiloLlamador = Thread.currentThread().getName();

        pushNotificationService.sendMulticastNotification(
                List.of("token-fake"), "Título", "Cuerpo", Map.of("k", "v"));

        long deadline = System.currentTimeMillis() + 2000;
        while (appender.list.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }

        assertThat(appender.list).as("el método debió loguear algo, sea en el hilo llamador o en el async").isNotEmpty();
        String hiloEjecutor = appender.list.get(0).getThreadName();
        assertThat(hiloEjecutor)
                .as("sendMulticastNotification debe ejecutarse en el pool notif-async-, no en el hilo que llamó a publicar()")
                .startsWith("notif-async-")
                .isNotEqualTo(hiloLlamador);
    }
}
