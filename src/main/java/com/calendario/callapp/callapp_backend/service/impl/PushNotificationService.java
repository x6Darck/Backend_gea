package com.calendario.callapp.callapp_backend.service.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
/**
 * Envío de notificaciones push a dispositivos móviles mediante Firebase Cloud Messaging (FCM).
 *
 * <p>En {@code @PostConstruct} intenta inicializar el Firebase Admin SDK cargando
 * {@code resources/firebase-service-account.json}. Si el archivo no existe (entorno de desarrollo
 * sin credenciales reales), el SDK no se inicializa y las notificaciones se simulan en los logs
 * sin lanzar excepción, permitiendo que el resto del backend funcione con normalidad.</p>
 */
public class PushNotificationService {

    @PostConstruct
    public void init() {
        try {
            // Cargar credenciales de Firebase desde el archivo config en resources
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
            if (serviceAccount != null) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    log.info("🔥 Firebase Admin SDK inicializado exitosamente.");
                }
            } else {
                log.warn("⚠️ Archivo firebase-service-account.json no encontrado en resources. Las notificaciones push serán simuladas en los logs.");
            }
        } catch (Exception e) {
            log.error("❌ Error al inicializar Firebase Admin SDK: {}", e.getMessage(), e);
        }
    }

    /**
     * Envía una notificación push llamativa a una lista de tokens de dispositivos.
     *
     * <p>{@code @Async}: se llama desde {@code SolicitudEventoServiceImpl.publicar()}
     * (transaccional); antes corría síncrona en ese mismo hilo, alargando la respuesta
     * HTTP de "publicar evento" en proporción al número de dispositivos registrados.
     * Usa el mismo pool que el resto de notificaciones (ver {@code AsyncConfig}).</p>
     */
    @Async("notificacionesExecutor")
    public void sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            log.info("No hay tokens de dispositivos registrados para enviar notificaciones.");
            return;
        }

        log.info("Preparando envío de notificación push a {} dispositivos...", tokens.size());

        if (FirebaseApp.getApps().isEmpty()) {
            log.info("📢 [PUSH SIMULADO] Destinatarios: {} tokens. Título: '{}'. Mensaje: '{}'. Datos: {}", 
                     tokens.size(), title, body, data);
            return;
        }

        try {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .addAllTokens(tokens)
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("✅ Notificaciones push enviadas. Éxito: {}, Falla: {}", 
                     response.getSuccessCount(), response.getFailureCount());
        } catch (Exception e) {
            log.error("❌ Error al enviar notificación push multicast de Firebase: {}", e.getMessage(), e);
        }
    }
}
