package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.response.DashboardResumenResponse;
import com.calendario.callapp.callapp_backend.dto.response.PublicacionEventoResponse;
import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.OficinaRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudAnuncioRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudEventoRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Agregación de métricas para el panel de control (dashboard).
 *
 * <p>El alcance de los conteos depende del rol del usuario autenticado:
 * {@code SUPER_ADMIN} y {@code ADMIN} ven totales globales (todas las oficinas),
 * mientras que {@code COMUNICACIONES} y {@code OFICINA} ven sólo los datos
 * de su propia oficina. También devuelve los próximos eventos publicados
 * delegando en {@link SolicitudEventoServiceImpl#listarProximos}.</p>
 */
public class DashboardServiceImpl {

    private final SolicitudEventoRepository solicitudEventoRepository;
    private final SolicitudAnuncioRepository solicitudAnuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final OficinaRepository oficinaRepository;
    private final SolicitudEventoServiceImpl solicitudEventoService;

    @Transactional(readOnly = true)
    public DashboardResumenResponse resumen(Authentication authentication) {
        Usuario usuario = usuarioRepository.getByCorreoOptimized(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));

        long totalSolicitudesEvento;
        long eventosPendientes;
        long eventosAprobados;
        long eventosPublicados;
        
        long totalSolicitudesAnuncio;
        long anunciosPendientes;
        long anunciosAprobados;
        long anunciosPublicados;
        
        long totalUsuarios;
        long totalOficinas;
        String alcance;

        if (usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones()) {
            totalSolicitudesEvento = solicitudEventoRepository.count();
            java.util.Map<EstadoSolicitud, Long> eventoCounts = mapCounts(solicitudEventoRepository.countByEstadoGrouped());
            eventosPendientes = eventoCounts.getOrDefault(EstadoSolicitud.PENDIENTE, 0L);
            eventosAprobados = eventoCounts.getOrDefault(EstadoSolicitud.APROBADA, 0L);
            eventosPublicados = eventoCounts.getOrDefault(EstadoSolicitud.PUBLICADA, 0L);
            
            totalSolicitudesAnuncio = solicitudAnuncioRepository.count();
            java.util.Map<EstadoSolicitud, Long> anuncioCounts = mapCounts(solicitudAnuncioRepository.countByEstadoGrouped());
            anunciosPendientes = anuncioCounts.getOrDefault(EstadoSolicitud.PENDIENTE, 0L);
            anunciosAprobados = anuncioCounts.getOrDefault(EstadoSolicitud.APROBADA, 0L);
            anunciosPublicados = anuncioCounts.getOrDefault(EstadoSolicitud.PUBLICADA, 0L);
            
            totalUsuarios = usuarioRepository.count();
            totalOficinas = oficinaRepository.count();
            alcance = "GLOBAL";
        } else if (usuario.getRol().esOficina()) {
            Oficina oficina = usuario.getOficina();
            if (oficina == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La oficina del usuario no esta configurada");
            }
            Long oficinaId = oficina.getId();
            
            totalSolicitudesEvento = solicitudEventoRepository.countByOficinaId(oficinaId);
            java.util.Map<EstadoSolicitud, Long> eventoCounts = mapCounts(solicitudEventoRepository.countByEstadoGroupedByOficina(oficinaId));
            eventosPendientes = eventoCounts.getOrDefault(EstadoSolicitud.PENDIENTE, 0L);
            eventosAprobados = eventoCounts.getOrDefault(EstadoSolicitud.APROBADA, 0L);
            eventosPublicados = eventoCounts.getOrDefault(EstadoSolicitud.PUBLICADA, 0L);
            
            totalSolicitudesAnuncio = solicitudAnuncioRepository.countByOficinaId(oficinaId);
            java.util.Map<EstadoSolicitud, Long> anuncioCounts = mapCounts(solicitudAnuncioRepository.countByEstadoGroupedByOficina(oficinaId));
            anunciosPendientes = anuncioCounts.getOrDefault(EstadoSolicitud.PENDIENTE, 0L);
            anunciosAprobados = anuncioCounts.getOrDefault(EstadoSolicitud.APROBADA, 0L);
            anunciosPublicados = anuncioCounts.getOrDefault(EstadoSolicitud.PUBLICADA, 0L);
            
            totalUsuarios = usuarioRepository.countByOficinaId(oficinaId);
            totalOficinas = 1;
            alcance = "OFICINA";
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para ver el dashboard administrativo");
        }

        List<PublicacionEventoResponse> proximosEventos = solicitudEventoService.listarProximos(10, null);

        return DashboardResumenResponse.builder()
                .alcance(alcance)
                .totalSolicitudesEvento(totalSolicitudesEvento)
                .eventosPendientes(eventosPendientes)
                .eventosAprobados(eventosAprobados)
                .eventosPublicados(eventosPublicados)
                .totalSolicitudesAnuncio(totalSolicitudesAnuncio)
                .anunciosPendientes(anunciosPendientes)
                .anunciosAprobados(anunciosAprobados)
                .anunciosPublicados(anunciosPublicados)
                .totalUsuarios(totalUsuarios)
                .totalOficinas(totalOficinas)
                .proximosEventos(proximosEventos)
                .build();
    }

    private java.util.Map<EstadoSolicitud, Long> mapCounts(List<Object[]> results) {
        java.util.Map<EstadoSolicitud, Long> map = new java.util.HashMap<>();
        if (results != null) {
            for (Object[] res : results) {
                map.put((EstadoSolicitud) res[0], (Long) res[1]);
            }
        }
        return map;
    }
}
