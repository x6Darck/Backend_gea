package com.calendario.callapp.callapp_backend.service.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un fallo transitorio de SMTP (conexión, timeout) no debe descartar el
 * correo en el primer intento: enviarCorreo debe reintentar con backoff
 * antes de darse por vencido y registrar el fallo.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.notifications.email-enabled=true",
        // MailHealthContributorAutoConfiguration falla al construirse cuando
        // JavaMailSender es un @MockBean ("Beans must not be empty") — no
        // relacionado con lo que este test verifica, solo lo desactivamos aquí.
        "management.health.mail.enabled=false"
})
class NotificacionServiceEmailRetryTest {

    @Autowired
    private NotificacionServiceImpl notificacionService;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void reintentaElEnvioTrasFallosTransitoriosYTerminaExitoso() throws InterruptedException {
        when(javaMailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));

        AtomicInteger intentos = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (intentos.incrementAndGet() < 3) {
                throw new MailSendException("Fallo transitorio simulado de SMTP");
            }
            return null; // Tercer intento: éxito, no lanza.
        }).when(javaMailSender).send(any(MimeMessage.class));

        notificacionService.notificarAprobacionEvento("Evento Reintento Test", "destino@unilibrecucuta.edu.co");

        long deadline = System.currentTimeMillis() + 5000;
        while (intentos.get() < 3 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertThat(intentos.get())
                .as("debió reintentar hasta el tercer intento antes de tener éxito")
                .isEqualTo(3);
        verify(javaMailSender, times(3)).send(any(MimeMessage.class));
    }
}
