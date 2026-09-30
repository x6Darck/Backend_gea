package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import com.calendario.callapp.callapp_backend.entity.MotivoFalloLogin;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import com.calendario.callapp.callapp_backend.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registra intentos de login (éxito/fallo) en {@link HistorialLogin} y purga los
 * antiguos por retención.
 *
 * <p>El registro se hace en una transacción {@code REQUIRES_NEW} independiente
 * de la de {@code AuthServiceImpl.login} (que es {@code @Transactional(readOnly=true)}
 * y LANZA una excepción en los fallos, así que registrar en su misma transacción
 * no persistiría). Esa transacción {@code REQUIRES_NEW} vive deliberadamente en
 * un bean aparte, {@link HistorialLoginPersister} — ver su Javadoc para el
 * porqué: en resumen, el try/catch de este servicio solo puede cubrir la
 * apertura y el commit de esa transacción (que Spring hace en el proxy AOP,
 * fuera del cuerpo del método interceptado) si la llamada transaccional cruza
 * a otro bean; con el {@code @Transactional} en el mismo método que hace el
 * try/catch, un fallo al abrir o comitear la transacción escaparía sin
 * capturar.</p>
 *
 * <p>La IP y el User-Agent se resuelven del request actual del hilo de forma
 * null-safe: si no hay request (ej. un test que llama directo al servicio) se
 * guardan null y el login sigue funcionando.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistorialLoginService {

    private final HistorialLoginPersister persister;
    private final HistorialLoginRepository repository;
    private final ClientIpResolver clientIpResolver;

    /** Días de retención antes de la purga automática. */
    private static final int RETENCION_DIAS = 90;

    /** Longitud máxima de {@code correo_intentado} (columna VARCHAR(160) NOT NULL). */
    private static final int CORREO_MAX_LEN = 160;

    public void registrarExito(String correo, Long idUsuario, AuthProvider metodo) {
        guardar(correo, idUsuario, true, metodo, null);
    }

    public void registrarFallo(String correo, Long idUsuario, AuthProvider metodo, MotivoFalloLogin motivo) {
        guardar(correo, idUsuario, false, metodo, motivo);
    }

    /**
     * Búsqueda paginada para el panel de seguridad ({@code SeguridadController}).
     * Normaliza filtros vacíos a null y convierte {@code hasta} en un límite
     * exclusivo del día siguiente para que sea inclusivo del día indicado
     * (misma convención que {@code AgendaPdfService.exportarAgendaPdf}).
     */
    @Transactional(readOnly = true)
    public Page<HistorialLogin> buscar(String correo, String ip, Boolean exito, LocalDate desde, LocalDate hasta, Pageable pageable) {
        String correoFiltro = (correo != null && !correo.isBlank()) ? correo.trim() : null;
        String ipFiltro = (ip != null && !ip.isBlank()) ? ip.trim() : null;
        LocalDateTime desdeDt = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDt = hasta != null ? hasta.plusDays(1).atStartOfDay() : null; // 'hasta' inclusivo
        return repository.buscar(correoFiltro, ipFiltro, exito, desdeDt, hastaDt, pageable);
    }

    private void guardar(String correo, Long idUsuario, boolean exito,
                         AuthProvider metodo, MotivoFalloLogin motivo) {
        try {
            HistorialLogin h = new HistorialLogin();
            String correoSaneado = correo != null ? correo : "";
            if (correoSaneado.length() > CORREO_MAX_LEN) {
                correoSaneado = correoSaneado.substring(0, CORREO_MAX_LEN);
            }
            h.setCorreoIntentado(correoSaneado);
            h.setIdUsuario(idUsuario);
            h.setExito(exito);
            h.setMetodo(metodo);
            h.setMotivoFallo(motivo);
            h.setFecha(LocalDateTime.now());

            HttpServletRequest req = requestActual();
            if (req != null) {
                h.setIp(clientIpResolver.resolve(req));
                String ua = req.getHeader("User-Agent");
                if (ua != null && ua.length() > 400) ua = ua.substring(0, 400);
                h.setUserAgent(ua);
            }
            persister.guardarEnNuevaTransaccion(h);
        } catch (Exception e) {
            // El registro de auditoría nunca debe tumbar el login. Si falla, se
            // deja traza en el log de la app y se sigue.
            log.warn("No se pudo registrar el intento de login en historial: {}", e.getMessage());
        }
    }

    private HttpServletRequest requestActual() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return (attrs instanceof ServletRequestAttributes sra) ? sra.getRequest() : null;
    }

    /**
     * Purga diaria (03:30) de los registros anteriores a la retención. El
     * scheduling ya está habilitado en la app (ver TokenBlacklistService). No
     * necesita el mismo tratamiento cross-bean que {@code guardar()}: la invoca
     * directamente el scheduler de Spring, no un llamador que dependa de la
     * garantía de "nunca lanza".
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgarAntiguos() {
        int borrados = repository.deleteByFechaBefore(LocalDateTime.now().minusDays(RETENCION_DIAS));
        if (borrados > 0) {
            log.info("Historial de login: purgados {} registros anteriores a {} días", borrados, RETENCION_DIAS);
        }
    }
}
