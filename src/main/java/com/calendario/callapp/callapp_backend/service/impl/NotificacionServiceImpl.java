package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.entity.NotificacionEnviada;
import com.calendario.callapp.callapp_backend.repository.NotificacionEnviadaRepository;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * Envío asíncrono de notificaciones de correo electrónico y push.
 *
 * <p>Todos los métodos públicos están anotados con {@code @Async("notificacionesExecutor")}
 * para no bloquear el hilo de la solicitud HTTP. El flag {@code app.notifications.email-enabled}
 * controla si se envía correo real o sólo se simula en log (útil en desarrollo).
 * Cada intento de envío queda registrado en {@link com.calendario.callapp.callapp_backend.entity.NotificacionEnviada}.
 * Las plantillas HTML se renderizan con {@link PlantillaCorreoServiceImpl} y se envían
 * con {@link org.springframework.mail.javamail.JavaMailSender}.
 * Las notificaciones push delegan en {@link PushNotificationService} (Firebase FCM).</p>
 */
public class NotificacionServiceImpl {

    private final JavaMailSender javaMailSender;
    private final CorreoSenderService correoSenderService;
    private final NotificacionEnviadaRepository notificacionEnviadaRepository;
    private final PlantillaCorreoServiceImpl plantillaCorreoService;
    private final ArchivoServiceImpl archivoService;

    @Value("${app.notifications.email-enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:}")
    private String mailUsername = "";

    @Value("${app.notifications.university-recipients:}")
    private String universityRecipients = "";

    @Value("${app.notifications.published-recipients:}")
    private String publishedRecipients = "";

    @Value("${app.mail.from-name:CallApp}")
    private String mailFromName = "CallApp";

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl = "http://localhost:5173";

    @Async("notificacionesExecutor")
    public void notificarCreacionEvento(Long id, String titulo, String nombreSolicitante, String nombreOficina) {
        log.info("Notificando NUEVA SOLICITUD de evento: id={}, titulo={}", id, titulo);
        
        Set<String> destinatarios = parseRecipients(universityRecipients);
        if (destinatarios.isEmpty()) return;

        enviarCorreo(
                "NUEVA_SOLICITUD_EVENTO",
                destinatarios,
                "Nueva solicitud de evento: " + titulo,
                plantillaCorreoService.render("nueva-solicitud.html", Map.of(
                        "tipo", "Evento",
                        "titulo", safe(titulo),
                        "solicitante", safe(nombreSolicitante),
                        "oficina", safe(nombreOficina),
                        "fecha", formatFecha(LocalDateTime.now()),
                        "gestionUrl", baseUrl() + "/revision-solicitudes")),
                null);
    }

    @Async("notificacionesExecutor")
    public void notificarCreacionAnuncio(Long id, String titulo, String nombreSolicitante, String nombreOficina) {
        log.info("Notificando NUEVA SOLICITUD de anuncio: id={}, titulo={}", id, titulo);

        Set<String> destinatarios = parseRecipients(universityRecipients);
        if (destinatarios.isEmpty()) return;

        enviarCorreo(
                "NUEVA_SOLICITUD_ANUNCIO",
                destinatarios,
                "Nueva solicitud de anuncio: " + titulo,
                plantillaCorreoService.render("nueva-solicitud.html", Map.of(
                        "tipo", "Anuncio",
                        "titulo", safe(titulo),
                        "solicitante", safe(nombreSolicitante),
                        "oficina", safe(nombreOficina),
                        "fecha", formatFecha(LocalDateTime.now()),
                        "gestionUrl", baseUrl() + "/revision-anuncios")),
                null);
    }

    @Async("notificacionesExecutor")
    public void notificarPublicacionEvento(Long publicacionId, String titulo, String descripcion, String fechaEvento, String horaInicio, String horaFin, String lugar, String tipoEvento, String responsable, String oficina, String piezaGraficaUrl, String correoSolicitante) {
        log.info("Iniciando proceso de notificacion para EVENTO publicado: id={}, titulo={}", publicacionId, titulo);

        Set<String> destinatarios = new LinkedHashSet<>();
        if (correoSolicitante != null && !correoSolicitante.isBlank()) {
            destinatarios.add(correoSolicitante);
        }

        Set<String> backendRecipients = parseRecipients(universityRecipients);
        destinatarios.addAll(backendRecipients);

        Set<String> massRecipients = parseRecipients(publishedRecipients);
        destinatarios.addAll(massRecipients);

        log.info("Destinatarios finales para evento: {}", destinatarios);

        String horario = formatHoraAmPm(horaInicio) + " - " + formatHoraAmPm(horaFin);

        enviarCorreo(
                "EVENTO_PUBLICADO",
                destinatarios,
                "Evento publicado: " + titulo,
                plantillaCorreoService.render("evento-publicado.html", Map.of(
                        "titulo", safe(titulo),
                        "descripcion", safe(descripcion),
                        "fechaEvento", safe(fechaEvento),
                        "horario", safe(horario),
                        "lugar", safe(lugar),
                        "tipoEvento", safe(tipoEvento),
                        "responsable", safe(responsable),
                        "oficina", safe(oficina),
                        "piezaUrl", safe(piezaGraficaUrl),
                        "detalleUrl", baseUrl() + "/calendario?evento=" + publicacionId)),
                piezaGraficaUrl);
    }

    @Async("notificacionesExecutor")
    public void notificarPublicacionAnuncio(Long publicacionId, String titulo, String descripcion, String categoria, String vigencia, String lugar, String responsable, String piezaGraficaUrl, String correoSolicitante, String correoContacto) {
        log.info("Iniciando proceso de notificacion para ANUNCIO publicado: id={}, titulo={}", publicacionId, titulo);

        Set<String> destinatarios = new LinkedHashSet<>();
        if (correoSolicitante != null && !correoSolicitante.isBlank()) {
            destinatarios.add(correoSolicitante);
        }
        if (correoContacto != null && !correoContacto.isBlank()) {
            destinatarios.add(correoContacto);
        }

        Set<String> backendRecipients = parseRecipients(universityRecipients);
        destinatarios.addAll(backendRecipients);

        Set<String> massRecipients = parseRecipients(publishedRecipients);
        destinatarios.addAll(massRecipients);

        log.info("Destinatarios finales para anuncio: {}", destinatarios);

        enviarCorreo(
                "ANUNCIO_PUBLICADO",
                destinatarios,
                "Anuncio publicado: " + titulo,
                plantillaCorreoService.render("anuncio-publicado.html", Map.of(
                        "titulo", safe(titulo),
                        "descripcion", safe(descripcion),
                        "categoria", safe(categoria),
                        "vigencia", safe(vigencia),
                        "lugar", safe(lugar),
                        "responsable", safe(responsable),
                        "contacto", safe(correoContacto),
                        "piezaUrl", safe(piezaGraficaUrl),
                        "detalleUrl", baseUrl() + "/calendario?anuncio=" + publicacionId)),
                piezaGraficaUrl);
    }

    @Async("notificacionesExecutor")
    public void notificarRechazoEvento(String nombreEvento, String motivoRechazo, String correoSolicitante) {
        log.info("Notificando RECHAZO de evento: titulo={}", nombreEvento);
        
        if (correoSolicitante == null || correoSolicitante.isBlank()) return;

        enviarCorreo(
                "EVENTO_RECHAZADO",
                Set.of(correoSolicitante),
                "Solicitud de evento rechazada: " + nombreEvento,
                plantillaCorreoService.render("solicitud-rechazada.html", Map.of(
                        "titulo", safe(nombreEvento),
                        "motivo", safe(motivoRechazo),
                        "gestionUrl", baseUrl() + "/mis-solicitudes")),
                null);
    }

    @Async("notificacionesExecutor")
    public void notificarAprobacionEvento(String nombreEvento, String correoSolicitante) {
        log.info("Notificando APROBACION de evento: titulo={}", nombreEvento);
        
        if (correoSolicitante == null || correoSolicitante.isBlank()) return;

        enviarCorreo(
                "EVENTO_APROBADO",
                Set.of(correoSolicitante),
                "Solicitud de evento aprobada: " + nombreEvento,
                plantillaCorreoService.render("solicitud-aprobada.html", Map.of(
                        "titulo", safe(nombreEvento),
                        "gestionUrl", baseUrl() + "/mis-solicitudes")),
                null);
    }

    @Async("notificacionesExecutor")
    public void notificarRechazoAnuncio(String titulo, String motivoRechazo, String correoSolicitante) {
        log.info("Notificando RECHAZO de anuncio: titulo={}", titulo);
        
        if (correoSolicitante == null || correoSolicitante.isBlank()) return;

        enviarCorreo(
                "ANUNCIO_RECHAZADO",
                Set.of(correoSolicitante),
                "Solicitud de anuncio rechazada: " + titulo,
                plantillaCorreoService.render("solicitud-rechazada.html", Map.of(
                        "titulo", safe(titulo),
                        "motivo", safe(motivoRechazo),
                        "gestionUrl", baseUrl() + "/mis-anuncios")),
                null);
    }

    @Async("notificacionesExecutor")
    public void notificarAprobacionAnuncio(String titulo, String correoSolicitante) {
        log.info("Notificando APROBACION de anuncio: titulo={}", titulo);
        
        if (correoSolicitante == null || correoSolicitante.isBlank()) return;

        enviarCorreo(
                "ANUNCIO_APROBADO",
                Set.of(correoSolicitante),
                "Solicitud de anuncio aprobada: " + titulo,
                plantillaCorreoService.render("solicitud-aprobada.html", Map.of(
                        "titulo", safe(titulo),
                        "gestionUrl", baseUrl() + "/mis-anuncios")),
                null);
    }

    @Async("notificacionesExecutor")
    public void enviarCorreoPruebaEvento(String destinatario, String titulo, String lugar, String oficina) {
        String tituloSeguro = safe(titulo).isBlank() ? "Evento de prueba CallApp" : safe(titulo);
        String lugarSeguro = safe(lugar).isBlank() ? "Auditorio principal" : safe(lugar);
        String oficinaSegura = safe(oficina).isBlank() ? "Comunicaciones" : safe(oficina);

        enviarCorreo(
                "EVENTO_PRUEBA",
                Set.of(destinatario),
                "Prueba de correo: " + tituloSeguro,
                plantillaCorreoService.render("evento-publicado.html", Map.of(
                        "titulo", tituloSeguro,
                        "fechaEvento", LocalDate.now().toString(),
                        "lugar", lugarSeguro,
                        "oficina", oficinaSegura,
                        "piezaUrl", "",
                        "detalleUrl", baseUrl() + "/calendario")),
                null);
    }

    private void enviarCorreo(String tipo, Set<String> destinatarios, String asunto, String cuerpo, String piezaGraficaUrl) {
        Set<String> destinatariosLimpios = new LinkedHashSet<>(destinatarios);
        destinatariosLimpios.removeIf(item -> item == null || item.isBlank());

        log.info("Procesando envio de correo tipo {}. Habilitado={}, Host-Configurado={}",
                tipo, emailEnabled, (mailUsername != null && !mailUsername.isBlank()));

        if (!emailEnabled) {
            log.info("Notificaciones por correo desactivadas globalmente. Se omite envio para asunto: {}", asunto);
            registrarNotificacion(tipo, destinatariosLimpios, asunto, false, "EMAIL_DESACTIVADO");
            return;
        }

        if (mailUsername == null || mailUsername.isBlank() || mailUsername.startsWith("tu_correo")) {
            log.warn("SMTP no configurado (usuario: {}). Se omite envio para asunto: {}", mailUsername, asunto);
            registrarNotificacion(tipo, destinatariosLimpios, asunto, false, "SMTP_NO_CONFIGURADO");
            return;
        }

        if (destinatariosLimpios.isEmpty()) {
            log.warn("No hay destinatarios validos para la notificacion: {}", asunto);
            registrarNotificacion(tipo, destinatariosLimpios, asunto, false, "SIN_DESTINATARIOS");
            return;
        }

        try {
            log.info("Preparando mensaje MIME para destinatarios: {}", destinatariosLimpios);
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            String fromAddress = mailUsername;
            String fromName = mailFromName;
            String[] toAddresses = destinatariosLimpios.toArray(new String[0]);

            String safeFrom = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress : "unknown@callapp.com";
            String safeFromName = (fromName != null && !fromName.isBlank()) ? fromName : "CallApp";
            
            helper.setFrom(safeFrom, safeFromName);
            if (toAddresses != null && toAddresses.length > 0) {
                // Usar copia oculta (BCC) solo para notificaciones masivas para proteger la privacidad
                if ("EVENTO_PUBLICADO".equals(tipo) || "ANUNCIO_PUBLICADO".equals(tipo)) {
                    helper.setTo(safeFrom); // El "Para" principal es el propio sistema
                    helper.setBcc(toAddresses); // Copia oculta a todos
                } else {
                    // Para notificaciones directas (aprobado, rechazado, nueva solicitud), enviarlo directamente al 'Para'
                    helper.setTo(toAddresses);
                }
            } else {
                helper.setTo(safeFrom);
            }
            helper.setSubject(asunto != null ? asunto : "Sin asunto");
            helper.setText(cuerpo != null ? cuerpo : "", true);

            if (piezaGraficaUrl != null && piezaGraficaUrl.contains("/archivos/public/")) {
                try {
                    String token = piezaGraficaUrl.substring(piezaGraficaUrl.lastIndexOf('/') + 1);
                    Resource recurso = archivoService.cargarPublicoComoRecurso(token);
                    if (recurso != null && recurso.exists()) {
                        String fileName = recurso.getFilename() != null ? recurso.getFilename() : "pieza_grafica.png";
                        
                        // Determinar si es imagen para usar inline o attachment
                        String contentType = Files.probeContentType(recurso.getFile().toPath());
                        
                        // Fallback manual si el sistema no reconoce la extension (comun en Windows)
                        if (contentType == null && fileName != null) {
                            String lowerName = fileName.toLowerCase();
                            if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || 
                                lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif") || 
                                lowerName.endsWith(".webp")) {
                                contentType = "image/" + (lowerName.endsWith(".png") ? "png" : "jpeg");
                            }
                        }

                        if (contentType != null && contentType.startsWith("image/")) {
                            helper.addInline("piezaGrafica", recurso);
                            log.info("Imagen incrustada (inline) en el correo: {} (Type: {})", fileName, contentType);
                        } else {
                            helper.addAttachment(java.util.Objects.requireNonNull(fileName), recurso);
                            log.info("Adjunto añadido al correo: {} (Type: {})", fileName, contentType);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("No se pudo procesar la pieza grafica para el correo: {}", ex.getMessage());
                }
            }

            log.info("Enviando correo vía SMTP...");
            correoSenderService.enviarConReintento(message);
            log.info("¡Correo enviado con exito para asunto: {}!", asunto);

            registrarNotificacion(tipo, destinatariosLimpios, asunto, true, null);
        } catch (Exception ex) {
            log.error("Error critico al enviar correo '{}' a {}: {}", asunto, destinatariosLimpios, ex.getMessage(), ex);
            registrarNotificacion(tipo, destinatariosLimpios, asunto, false, ex.getMessage());
        }
    }

    private Set<String> parseRecipients(String rawRecipients) {
        if (rawRecipients == null || rawRecipients.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rawRecipients.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private void registrarNotificacion(String tipo, Set<String> destinatarios, String asunto, boolean exito,
            String detalleError) {
        NotificacionEnviada notificacion = new NotificacionEnviada();
        notificacion.setTipo(tipo);
        notificacion.setDestinatarios(String.join(",", destinatarios));
        notificacion.setAsunto(asunto);
        notificacion.setFechaEnvio(LocalDateTime.now());
        notificacion.setExito(exito);
        notificacion.setDetalleError(detalleError);
        notificacionEnviadaRepository.save(notificacion);
    }

    void configurarParaPruebas(
            boolean emailEnabled,
            String mailUsername,
            String mailFromName,
            String frontendBaseUrl,
            String universityRecipients) {
        this.emailEnabled = emailEnabled;
        this.mailUsername = mailUsername;
        this.mailFromName = mailFromName;
        this.frontendBaseUrl = frontendBaseUrl;
        this.universityRecipients = universityRecipients;
    }

    /** Devuelve la primera URL de app.frontend-base-url (puede ser lista separada por comas). */
    private String baseUrl() {
        String v = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        int comma = v.indexOf(',');
        return (comma >= 0 ? v.substring(0, comma) : v).trim();
    }

    private String safe(String valor) {
        return valor == null ? "" : valor;
    }

    private String formatFecha(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", new Locale("es", "ES")));
    }

    private String formatHoraAmPm(String hora) {
        if (hora == null || hora.isBlank()) return "";
        try {
            return LocalTime.parse(hora).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
        } catch (Exception e) {
            return hora;
        }
    }
}
