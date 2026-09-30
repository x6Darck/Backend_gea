package com.calendario.callapp.callapp_backend.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Envío de correo con reintento — bean separado a propósito.
 *
 * <p>{@code @Retryable} funciona interceptando la llamada vía un proxy
 * (Spring AOP); una llamada interna {@code this.metodo()} dentro de la
 * misma clase NO pasa por el proxy y el reintento se ignora en silencio
 * (limitación conocida de auto-invocación en Spring AOP). Por eso este
 * método vive en un bean aparte, invocado desde {@link NotificacionServiceImpl}
 * a través de la referencia inyectada (que sí pasa por el proxy).</p>
 */
@Service
@RequiredArgsConstructor
public class CorreoSenderService {

    private final JavaMailSender javaMailSender;

    /**
     * Un fallo de {@link MailSendException} (conexión, timeout) suele ser
     * transitorio — SMTP puede recuperarse en segundos. Reintenta hasta 3
     * veces con backoff exponencial (1s, 2s, 4s) antes de propagar el fallo
     * al llamador, que lo registra como notificación fallida.
     */
    @Retryable(retryFor = MailSendException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void enviarConReintento(MimeMessage message) {
        javaMailSender.send(message);
    }
}
