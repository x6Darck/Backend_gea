package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.DevolucionRequest;
import com.calendario.callapp.callapp_backend.dto.request.PublicacionAnuncioRequest;
import com.calendario.callapp.callapp_backend.dto.request.RechazoRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudAnuncioRequest;
import com.calendario.callapp.callapp_backend.dto.request.UpdatePublicacionRequest;
import com.calendario.callapp.callapp_backend.dto.response.PublicacionAnuncioResponse;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudAnuncioResponse;
import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.entity.PublicacionAnuncio;
import com.calendario.callapp.callapp_backend.entity.SolicitudAnuncio;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.LugarFisicoRepository;
import com.calendario.callapp.callapp_backend.repository.OficinaRepository;
import com.calendario.callapp.callapp_backend.repository.PublicacionAnuncioRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudAnuncioRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.calendario.callapp.callapp_backend.mapper.PublicacionAnuncioMapper;
import com.calendario.callapp.callapp_backend.mapper.SolicitudAnuncioMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
/**
 * Ciclo de vida de solicitudes de anuncio (sin recurrencia ni control de espacio físico).
 *
 * <p>Misma máquina de estados que eventos ({@code PENDIENTE → EN_REVISION → APROBADA → PUBLICADA})
 * pero con las simplificaciones propias de un anuncio: no hay conflicto de lugar, no hay
 * participantes y la publicación incluye un rango de vigencia
 * ({@code fechaInicioPublicacion} / {@code fechaFinPublicacion}).
 * Al aprobar o publicar dispara notificaciones asíncronas a través de
 * {@link NotificacionServiceImpl}.</p>
 */
public class SolicitudAnuncioServiceImpl {

    private final SolicitudAnuncioRepository solicitudAnuncioRepository;
    private final PublicacionAnuncioRepository publicacionAnuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final OficinaRepository oficinaRepository;
    private final NotificacionServiceImpl notificacionService;
    private final SolicitudAnuncioMapper solicitudAnuncioMapper;
    private final LugarFisicoRepository lugarFisicoRepository;
    private final PublicacionAnuncioMapper publicacionAnuncioMapper;

    @Transactional
    public SolicitudAnuncioResponse crear(SolicitudAnuncioRequest request, Authentication authentication) {
        Usuario usuario = obtenerUsuario(authentication);
        
        Oficina oficina = usuario.getOficina();
        // Lógica de oficina para creación: si es estudiante, forzar nulo. Si es admin/comms sin oficina, asignar la primera.
        if (usuario.getRol() == com.calendario.callapp.callapp_backend.entity.Rol.USUARIO_AUTENTICADO_APP) {
            oficina = null;
        } else if (oficina == null && (usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones())) {
            oficina = oficinaRepository.findAll().stream().findFirst().orElse(null);
        }

        SolicitudAnuncio solicitud = new SolicitudAnuncio();
        solicitud.setTitulo(request.getTitulo());
        solicitud.setDescripcion(request.getDescripcion());
        solicitud.setCategoria(request.getCategoria());
        if (request.getIdsLugaresFisicos() != null && !request.getIdsLugaresFisicos().isEmpty()) {
            java.util.List<Long> idsSeguros = request.getIdsLugaresFisicos().stream().map(Number::longValue).toList();
            solicitud.getLugaresFisicos().clear();
            solicitud.getLugaresFisicos().addAll(lugarFisicoRepository.findAllById(java.util.Objects.requireNonNull(idsSeguros)));
        }
        solicitud.setCorreoContacto(request.getCorreoContacto());
        solicitud.setResponsableAnuncio(request.getResponsableAnuncio());
        solicitud.setFechaInicioPublicacion(request.getFechaInicioPublicacion());
        solicitud.setFechaFinPublicacion(request.getFechaFinPublicacion());
        solicitud.setHoraInicio(request.getHoraInicio());
        solicitud.setHoraFin(request.getHoraFin());
        solicitud.setPiezaGraficaUrl(request.getPiezaGraficaUrl());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setUsuarioSolicitante(usuario);
        solicitud.setOficina(oficina);
        solicitud.setRequierePiezaGrafica(request.getRequierePiezaGrafica() != null ? request.getRequierePiezaGrafica() : false);

        SolicitudAnuncio guardada = solicitudAnuncioRepository.save(solicitud);

        notificacionService.notificarCreacionAnuncio(
            guardada.getId(),
            guardada.getTitulo(),
            guardada.getUsuarioSolicitante() != null ? guardada.getUsuarioSolicitante().getNombre() : "N/A",
            guardada.getOficina() != null ? guardada.getOficina().getNombre() : "N/A"
        );

        return solicitudAnuncioMapper.toResponse(guardada);
    }

    @Transactional
    public SolicitudAnuncioResponse actualizar(Long id, SolicitudAnuncioRequest request, Authentication authentication) {
        SolicitudAnuncio solicitud = buscarSolicitud(id);
        Usuario usuario = obtenerUsuario(authentication);

        boolean isOwner = solicitud.getUsuarioSolicitante() != null && solicitud.getUsuarioSolicitante().getId().equals(usuario.getId());
        boolean isAdmin = usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones();

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para editar esta solicitud");
        }

        if (isOwner && !isAdmin && solicitud.getEstado() == EstadoSolicitud.RECHAZADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Esta solicitud fue rechazada definitivamente y no se puede reenviar. Solo las solicitudes en revisión pueden editarse y reenviarse.");
        }

        if (request.getTitulo() != null) solicitud.setTitulo(request.getTitulo());
        if (request.getDescripcion() != null) solicitud.setDescripcion(request.getDescripcion());
        if (request.getCategoria() != null) solicitud.setCategoria(request.getCategoria());
        if (request.getIdsLugaresFisicos() != null) {
            java.util.List<Long> idsSeguros = request.getIdsLugaresFisicos().stream().map(Number::longValue).toList();
            solicitud.getLugaresFisicos().clear();
            solicitud.getLugaresFisicos().addAll(lugarFisicoRepository.findAllById(java.util.Objects.requireNonNull(idsSeguros)));
        }
        if (request.getCorreoContacto() != null) solicitud.setCorreoContacto(request.getCorreoContacto());
        if (request.getResponsableAnuncio() != null) solicitud.setResponsableAnuncio(request.getResponsableAnuncio());
        if (request.getFechaInicioPublicacion() != null) solicitud.setFechaInicioPublicacion(request.getFechaInicioPublicacion());
        if (request.getFechaFinPublicacion() != null) solicitud.setFechaFinPublicacion(request.getFechaFinPublicacion());
        if (request.getHoraInicio() != null) solicitud.setHoraInicio(request.getHoraInicio());
        if (request.getHoraFin() != null) solicitud.setHoraFin(request.getHoraFin());
        if (request.getPiezaGraficaUrl() != null) solicitud.setPiezaGraficaUrl(request.getPiezaGraficaUrl());

        boolean devuelta = solicitud.getEstado() == EstadoSolicitud.EN_REVISION;
        if (isOwner && devuelta) {
            solicitud.setEstado(EstadoSolicitud.PENDIENTE);
            solicitud.setMotivoRechazo(null);
            solicitud.setObservacionesRevision(null);
            solicitud.setUsuarioRevisor(null);
            solicitud.setFechaRevision(null);
        }

        return enrichResponse(solicitudAnuncioMapper.toResponse(solicitudAnuncioRepository.save(solicitud)));
    }


    @Transactional(readOnly = true)
    public List<SolicitudAnuncioResponse> listarPropias(
            Authentication authentication,
            String q,
            EstadoSolicitud estado,
            Integer mes,
            Integer anio) {
        Usuario usuario = obtenerUsuario(authentication);
        
        if (usuario.getOficina() != null) {
            return solicitudAnuncioMapper.toResponseList(
                solicitudAnuncioRepository.getAllByOficinaIdOptimized(usuario.getOficina().getId()).stream()
                    .filter(solicitud -> coincideFiltroSolicitud(solicitud, q, estado, mes, anio))
                    .toList()
            );
        }

        return enrichResponseList(solicitudAnuncioRepository.getAllUniqueWithAssociations(PageRequest.of(0, 1000)).stream()
                .filter(solicitud -> solicitud.getUsuarioSolicitante() != null && solicitud.getUsuarioSolicitante().getId().equals(usuario.getId()))
                .filter(solicitud -> coincideFiltroSolicitud(solicitud, q, estado, mes, anio))
                .map(solicitudAnuncioMapper::toResponse)
                .toList());
    }


    @Transactional(readOnly = true)
    public List<SolicitudAnuncioResponse> listarParaRevision(String q, EstadoSolicitud estado, Integer mes, Integer anio) {
        return enrichResponseList(solicitudAnuncioMapper.toResponseList(
            solicitudAnuncioRepository.getAllUniqueWithAssociations(PageRequest.of(0, 1000)).stream()
                .filter(solicitud -> coincideFiltroSolicitud(solicitud, q, estado, mes, anio))
                .toList()
        ));
    }


    @Transactional(readOnly = true)
    public SolicitudAnuncioResponse obtenerParaRevision(Long id) {
        return enrichResponse(solicitudAnuncioMapper.toResponse(buscarSolicitud(id)));
    }

    @Transactional(readOnly = true)
    public SolicitudAnuncioResponse obtenerUno(Long id) {
        return enrichResponse(solicitudAnuncioMapper.toResponse(buscarSolicitud(id)));
    }


    @Transactional
    public SolicitudAnuncioResponse aprobar(Long id, Authentication authentication) {
        SolicitudAnuncio solicitud = buscarSolicitud(id);
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE &&
            solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No se puede aprobar una solicitud en estado " + solicitud.getEstado().name());
        }
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setMotivoRechazo(null);
        solicitud.setUsuarioRevisor(obtenerUsuario(authentication));
        solicitud.setFechaRevision(LocalDateTime.now());
        SolicitudAnuncio guardada = solicitudAnuncioRepository.save(solicitud);

        notificacionService.notificarAprobacionAnuncio(
            guardada.getTitulo(),
            guardada.getUsuarioSolicitante() != null ? guardada.getUsuarioSolicitante().getCorreo() : null
        );

        return enrichResponse(solicitudAnuncioMapper.toResponse(guardada));
    }

    @Transactional
    public SolicitudAnuncioResponse rechazar(Long id, RechazoRequest request, Authentication authentication) {
        SolicitudAnuncio solicitud = buscarSolicitud(id);
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE &&
            solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No se puede rechazar una solicitud en estado " + solicitud.getEstado().name());
        }
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setMotivoRechazo(request.getMotivo());
        solicitud.setUsuarioRevisor(obtenerUsuario(authentication));
        solicitud.setFechaRevision(LocalDateTime.now());
        
        SolicitudAnuncio guardada = solicitudAnuncioRepository.save(solicitud);

        notificacionService.notificarRechazoAnuncio(
            guardada.getTitulo(),
            guardada.getMotivoRechazo(),
            guardada.getUsuarioSolicitante() != null ? guardada.getUsuarioSolicitante().getCorreo() : null
        );

        return solicitudAnuncioMapper.toResponse(guardada);
    }

    @Transactional
    public SolicitudAnuncioResponse devolver(Long id, DevolucionRequest request, Authentication authentication) {
        SolicitudAnuncio solicitud = buscarSolicitud(id);
        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
        solicitud.setObservacionesRevision(request.getObservaciones());
        solicitud.setUsuarioRevisor(obtenerUsuario(authentication));
        solicitud.setFechaRevision(LocalDateTime.now());

        SolicitudAnuncio guardada = solicitudAnuncioRepository.save(solicitud);
        return solicitudAnuncioMapper.toResponse(guardada);
    }

    @Transactional
    public PublicacionAnuncioResponse publicar(Long id, PublicacionAnuncioRequest request, Authentication authentication) {
        SolicitudAnuncio solicitud = buscarSolicitud(id);
        if (solicitud.getEstado() != EstadoSolicitud.APROBADA && solicitud.getEstado() != EstadoSolicitud.PUBLICADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo puedes publicar una solicitud aprobada");
        }

        Usuario publicador = obtenerUsuario(authentication);
        PublicacionAnuncio publicacion = publicacionAnuncioRepository.findBySolicitudAnuncioId(id).orElseGet(PublicacionAnuncio::new);
        publicacion.setSolicitudAnuncio(solicitud);
        publicacion.setTituloVisible(request.getTituloVisible() != null ? request.getTituloVisible() : solicitud.getTitulo());
        publicacion.setDescripcionVisible(request.getDescripcionVisible() != null ? request.getDescripcionVisible() : solicitud.getDescripcion());
        publicacion.setPiezaGraficaUrl(request.getPiezaGraficaUrl() != null ? request.getPiezaGraficaUrl() : solicitud.getPiezaGraficaUrl());
        publicacion.setFechaPublicacion(request.getFechaPublicacion() != null ? request.getFechaPublicacion() : LocalDateTime.now());
        publicacion.setVisible(true);
        publicacion.setUsuarioPublicador(publicador);

        solicitud.setEstado(EstadoSolicitud.PUBLICADA);
        solicitud.setPiezaGraficaUrl(publicacion.getPiezaGraficaUrl());
        solicitudAnuncioRepository.save(solicitud);

        PublicacionAnuncio guardada = publicacionAnuncioRepository.save(publicacion);
        
        String vigencia = (solicitud.getFechaInicioPublicacion() != null ? solicitud.getFechaInicioPublicacion().toString() : "") 
                        + " - " + (solicitud.getFechaFinPublicacion() != null ? solicitud.getFechaFinPublicacion().toString() : "");

        notificacionService.notificarPublicacionAnuncio(
            guardada.getId(),
            guardada.getTituloVisible(),
            solicitud.getDescripcion(),
            solicitud.getCategoria(),
            vigencia,
            solicitud.getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).collect(java.util.stream.Collectors.joining(", ")),
            solicitud.getResponsableAnuncio(),
            guardada.getPiezaGraficaUrl(),
            solicitud.getUsuarioSolicitante() != null ? solicitud.getUsuarioSolicitante().getCorreo() : null,
            solicitud.getCorreoContacto()
        );
        return publicacionAnuncioMapper.toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<PublicacionAnuncioResponse> listarPublicados(String q, String categoria, Integer mes, Integer anio) {
        return publicacionAnuncioRepository.findByVisibleTrueOrderByFechaPublicacionDesc().stream()
                .filter(publicacion -> coincideFiltroPublicacion(publicacion, q, categoria, mes, anio))
                .map(publicacionAnuncioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicacionAnuncioResponse obtenerPublicacion(Long id) {
        return publicacionAnuncioRepository.findBySolicitudAnuncioId(id)
                .map(publicacionAnuncioMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));
    }

    @Transactional
    public void eliminarSolicitud(Long id, Authentication authentication) {
        SolicitudAnuncio solicitud = buscarSolicitud(id);
        Usuario usuario = obtenerUsuario(authentication);
        
        boolean isOwner = solicitud.getUsuarioSolicitante() != null && 
                          solicitud.getUsuarioSolicitante().getId().equals(usuario.getId());
        boolean isAdmin = usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones();
        
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar esta solicitud de anuncio");
        }
        
        publicacionAnuncioRepository.findBySolicitudAnuncioId(id).ifPresent(publicacionAnuncioRepository::delete);
        solicitudAnuncioRepository.delete(solicitud);
    }

    @Transactional
    public void eliminarPublicacion(Long id) {
        PublicacionAnuncio publicacion = publicacionAnuncioRepository.findBySolicitudAnuncioId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));
        
        SolicitudAnuncio solicitud = publicacion.getSolicitudAnuncio();
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitudAnuncioRepository.save(solicitud);
        
        publicacionAnuncioRepository.delete(publicacion);
    }

    @Transactional
    public PublicacionAnuncioResponse toggleVisibilidad(Long id, boolean visible) {
        PublicacionAnuncio p = publicacionAnuncioRepository.findBySolicitudAnuncioId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));
        p.setVisible(visible);
        PublicacionAnuncio guardada = publicacionAnuncioRepository.save(p);
        return publicacionAnuncioMapper.toResponse(Objects.requireNonNull(guardada));
    }

    @Transactional
    @SuppressWarnings("null")
    public PublicacionAnuncioResponse updatePublicacion(Long id, UpdatePublicacionRequest request) {
        PublicacionAnuncio p = publicacionAnuncioRepository.findBySolicitudAnuncioId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));

        if (request.getTituloVisible() != null) p.setTituloVisible(request.getTituloVisible());
        if (request.getDescripcionVisible() != null) p.setDescripcionVisible(request.getDescripcionVisible());
        if (request.getPiezaGraficaUrl() != null) p.setPiezaGraficaUrl(request.getPiezaGraficaUrl());

        // Actualizar solicitud subyacente (Sincronización Administrativa)
        SolicitudAnuncio solicitud = p.getSolicitudAnuncio();
        if (request.getTitulo() != null) solicitud.setTitulo(request.getTitulo());
        if (request.getDescripcion() != null) solicitud.setDescripcion(request.getDescripcion());
        if (request.getCategoria() != null) solicitud.setCategoria(request.getCategoria());
        if (request.getIdsLugaresFisicos() != null) {
            solicitud.getLugaresFisicos().clear();
            solicitud.getLugaresFisicos().addAll(lugarFisicoRepository.findAllById(Objects.requireNonNull(request.getIdsLugaresFisicos())));
        }
        if (request.getCorreoContacto() != null) solicitud.setCorreoContacto(request.getCorreoContacto());
        if (request.getResponsableAnuncio() != null) solicitud.setResponsableAnuncio(request.getResponsableAnuncio());
        if (request.getFechaInicioPublicacion() != null) solicitud.setFechaInicioPublicacion(request.getFechaInicioPublicacion());
        if (request.getFechaFinPublicacion() != null) solicitud.setFechaFinPublicacion(request.getFechaFinPublicacion());
        if (request.getHoraInicio() != null) solicitud.setHoraInicio(request.getHoraInicio());
        if (request.getHoraFin() != null) solicitud.setHoraFin(request.getHoraFin());
        if (request.getPiezaGraficaUrl() != null) solicitud.setPiezaGraficaUrl(request.getPiezaGraficaUrl());

        if (request.getIdOficina() != null) {
            Oficina ofi = oficinaRepository.findById(request.getIdOficina())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oficina no encontrada"));
            solicitud.setOficina(ofi);
        }

        solicitudAnuncioRepository.save(solicitud);
        PublicacionAnuncio guardada = publicacionAnuncioRepository.save(p);
        return publicacionAnuncioMapper.toResponse(Objects.requireNonNull(guardada));
    }


    private SolicitudAnuncio buscarSolicitud(Long id) {
        return solicitudAnuncioRepository.getByIdOptimized(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud de anuncio no encontrada"));
    }

    private Usuario obtenerUsuario(Authentication authentication) {
        return usuarioRepository.getByCorreoOptimized(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));
    }

    private boolean coincideFiltroSolicitud(
            SolicitudAnuncio solicitud,
            String q,
            EstadoSolicitud estado,
            Integer mes,
            Integer anio) {
        if (estado != null && solicitud.getEstado() != estado) return false;
        if (mes != null && solicitud.getFechaCreacion().getMonthValue() != mes) return false;
        if (anio != null && solicitud.getFechaCreacion().getYear() != anio) return false;
        if (q == null || q.isBlank()) return true;

        String criterio = q.toLowerCase(Locale.ROOT).trim();
        return contiene(solicitud.getTitulo(), criterio)
                || contiene(solicitud.getDescripcion(), criterio)
                || contiene(solicitud.getCategoria(), criterio)
                || contiene(solicitud.getEstado().name(), criterio)
                || (solicitud.getUsuarioSolicitante() != null && contiene(solicitud.getUsuarioSolicitante().getCorreo(), criterio));
    }

    private boolean coincideFiltroPublicacion(
            PublicacionAnuncio publicacion,
            String q,
            String categoria,
            Integer mes,
            Integer anio) {
        SolicitudAnuncio solicitud = publicacion.getSolicitudAnuncio();
        if (categoria != null && !categoria.isBlank()
                && (solicitud.getCategoria() == null || !solicitud.getCategoria().equalsIgnoreCase(categoria.trim()))) {
            return false;
        }
        if (mes != null && publicacion.getFechaPublicacion().getMonthValue() != mes) return false;
        if (anio != null && publicacion.getFechaPublicacion().getYear() != anio) return false;
        if (q == null || q.isBlank()) return true;

        String criterio = q.toLowerCase(Locale.ROOT).trim();
        return contiene(publicacion.getTituloVisible(), criterio)
                || contiene(publicacion.getDescripcionVisible(), criterio)
                || contiene(solicitud.getCategoria(), criterio)
                || contiene(solicitud.getTitulo(), criterio);
    }

    private boolean contiene(String valor, String criterio) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(criterio);
    }

    // --- ENRIQUECIMIENTO DE RESPUESTAS CON VISIBILIDAD ---

    private SolicitudAnuncioResponse enrichResponse(SolicitudAnuncioResponse response) {
        if (response == null) return null;
        publicacionAnuncioRepository.findBySolicitudAnuncioId(response.getId())
                .ifPresent(p -> {
                    response.setVisible(p.getVisible());
                    if (response.getPiezaGraficaUrl() == null || response.getPiezaGraficaUrl().isBlank()) {
                        response.setPiezaGraficaUrl(p.getPiezaGraficaUrl());
                    }
                });
        return response;
    }

    private List<SolicitudAnuncioResponse> enrichResponseList(List<SolicitudAnuncioResponse> responses) {
        if (responses == null || responses.isEmpty()) return responses;

        List<Long> ids = responses.stream().map(SolicitudAnuncioResponse::getId).toList();
        List<PublicacionAnuncio> publicaciones = publicacionAnuncioRepository.findBySolicitudAnuncioIdIn(ids);

        java.util.Map<Long, PublicacionAnuncio> publicationMap = publicaciones.stream()
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getSolicitudAnuncio().getId(),
                        p -> p,
                        (p1, p2) -> p1
                ));

        responses.forEach(r -> {
            PublicacionAnuncio p = publicationMap.get(r.getId());
            if (p != null) {
                r.setVisible(p.getVisible());
                if (r.getPiezaGraficaUrl() == null || r.getPiezaGraficaUrl().isBlank()) {
                    r.setPiezaGraficaUrl(p.getPiezaGraficaUrl());
                }
            } else {
                r.setVisible(false);
            }
        });
        return responses;
    }
}

