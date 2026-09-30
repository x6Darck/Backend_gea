package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.PublicacionEventoRequest;
import com.calendario.callapp.callapp_backend.dto.request.DevolucionRequest;
import com.calendario.callapp.callapp_backend.dto.request.RechazoRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoParticipanteRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoRequest;
import com.calendario.callapp.callapp_backend.dto.request.UpdatePublicacionRequest;
import com.calendario.callapp.callapp_backend.dto.response.PublicacionEventoResponse;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudEventoResponse;
import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.FrecuenciaRecurrencia;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.entity.PublicacionEvento;
import com.calendario.callapp.callapp_backend.entity.LugarFisico;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import com.calendario.callapp.callapp_backend.entity.SolicitudEventoParticipante;
import com.calendario.callapp.callapp_backend.entity.TipoEventoCatalogo;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.LugarFisicoRepository;
import com.calendario.callapp.callapp_backend.repository.OficinaRepository;
import com.calendario.callapp.callapp_backend.repository.PublicacionEventoRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudEventoRepository;
import com.calendario.callapp.callapp_backend.repository.TipoEventoCatalogoRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudEventoParticipanteRepository;
import com.calendario.callapp.callapp_backend.repository.DispositivoUsuarioRepository;
import com.calendario.callapp.callapp_backend.mapper.PublicacionEventoMapper;
import com.calendario.callapp.callapp_backend.mapper.SolicitudEventoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * Servicio principal del ciclo de vida de solicitudes de evento.
 *
 * <p>Implementa la máquina de estados:
 * {@code PENDIENTE → EN_REVISION → APROBADA → PUBLICADA}, con desvíos a
 * {@code RECHAZADA} y retorno a {@code PENDIENTE} mediante devolución.
 * Responsabilidades clave:</p>
 * <ul>
 *   <li><b>Creación:</b> valida horario, detecta conflictos de espacio con
 *       {@code SolicitudEventoRepository.findConflictsBulk} (solapamiento de intervalos),
 *       y genera la serie recurrente asignando un {@code idGrupoRecurrencia} UUID compartido.</li>
 *   <li><b>Participantes:</b> usa patrón «borrar-y-reinsertar» via
 *       {@link com.calendario.callapp.callapp_backend.repository.SolicitudEventoParticipanteRepository}.</li>
 *   <li><b>Publicación:</b> crea la {@link com.calendario.callapp.callapp_backend.entity.PublicacionEvento}
 *       y dispara notificaciones asíncronas de email y push.</li>
 *   <li><b>Series recurrentes:</b> las operaciones de edición y cancelación propagan
 *       al grupo completo cuando el cliente lo solicita.</li>
 * </ul>
 */
public class SolicitudEventoServiceImpl {

    private final SolicitudEventoRepository solicitudEventoRepository;
    private final PublicacionEventoRepository publicacionEventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OficinaRepository oficinaRepository;
    private final TipoEventoCatalogoRepository tipoEventoCatalogoRepository;
    private final SolicitudEventoParticipanteRepository solicitudEventoParticipanteRepository;
    private final LugarFisicoRepository lugarFisicoRepository;
    private final NotificacionServiceImpl notificacionService;
    private final SolicitudEventoMapper solicitudEventoMapper;
    private final PublicacionEventoMapper publicacionEventoMapper;
    private final PushNotificationService pushNotificationService;
    private final DispositivoUsuarioRepository dispositivoUsuarioRepository;


    @Transactional
    @SuppressWarnings("null")
    public SolicitudEventoResponse crear(SolicitudEventoRequest request, Authentication authentication) {
        validarHorario(request);
        if (request.getFechaEvento().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha del evento no puede ser en el pasado");
        }

        Usuario usuario = obtenerUsuario(authentication);
        if (!usuario.getRol().esOficina() && !usuario.getRol().esAdministradorGlobal()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu rol no tiene permiso para crear solicitudes de evento");
        }
        
        Oficina oficina = usuario.getOficina();

        // Si el usuario es admin y envía un idOficina, respetarlo
        if ((usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones()) && request.getIdOficina() != null) {
            oficina = oficinaRepository.findById(request.getIdOficina())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oficina seleccionada no encontrada"));
        } else if (oficina == null && (usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones())) {
            // Fallback si no envía idOficina: primera oficina existente
            oficina = oficinaRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay oficinas configuradas en el sistema para asociar este evento administrativo"));
        } else if (oficina == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no tiene oficina asociada");
        }
        TipoEventoCatalogo tipoEvento = resolveTipoEvento(request.getTipoEvento());

        SolicitudEvento solicitudOriginal = new SolicitudEvento();
        mapearSolicitudBase(solicitudOriginal, request, tipoEvento, oficina, usuario);
        
        checkConflicts(solicitudOriginal.getFechaEvento(), solicitudOriginal.getLugaresFisicos(), solicitudOriginal.getHoraInicio(), solicitudOriginal.getHoraFin(), null);

        guardarParticipantes(solicitudOriginal, request.getParticipantes());

        // Manejo de Recurrencia: asignar grupo/maestra ANTES del único save para evitar
        // un maestro huérfano si la generación de instancias falla (rollback transaccional).
        boolean esRecurrente = request.getFrecuenciaRecurrencia() != null &&
            request.getFrecuenciaRecurrencia() != com.calendario.callapp.callapp_backend.entity.FrecuenciaRecurrencia.NINGUNA &&
            request.getFechaFinRecurrencia() != null;
        if (esRecurrente) {
            solicitudOriginal.setIdGrupoRecurrencia(java.util.UUID.randomUUID().toString());
            solicitudOriginal.setEsPrincipal(true); // La primera es la maestra
        }

        SolicitudEvento guardada = solicitudEventoRepository.save(solicitudOriginal);

        if (esRecurrente) {
            generarInstanciasDesde(
                guardada,
                guardada.getFrecuenciaRecurrencia(),
                guardada.getFechaFinRecurrencia(),
                tipoEvento, oficina, usuario,
                request.getParticipantes());
        }
        
        notificacionService.notificarCreacionEvento(
            guardada.getId(),
            guardada.getNombreEvento(),
            guardada.getUsuarioSolicitante() != null ? guardada.getUsuarioSolicitante().getNombre() : "N/A",
            guardada.getOficina() != null ? guardada.getOficina().getNombre() : "N/A"
        );

        return enrichResponse(solicitudEventoMapper.toResponse(guardada));
    }

    private void mapearSolicitudBase(SolicitudEvento solicitud, SolicitudEventoRequest request, TipoEventoCatalogo tipoEvento, Oficina oficina, Usuario usuario) {
        solicitud.setNombreEvento(request.getNombreEvento());
        solicitud.setDescripcionEvento(request.getDescripcionEvento());
        solicitud.setFechaEvento(request.getFechaEvento());
        solicitud.setHoraInicio(request.getHoraInicio());
        solicitud.setHoraFin(request.getHoraFin());
        if (request.getIdsLugaresFisicos() != null && !request.getIdsLugaresFisicos().isEmpty()) {
            solicitud.getLugaresFisicos().clear();
            solicitud.getLugaresFisicos().addAll(lugarFisicoRepository.findAllById(Objects.requireNonNull(request.getIdsLugaresFisicos())));
        } else {
            solicitud.getLugaresFisicos().clear();
        }
        solicitud.setLinkConexion(request.getLinkConexion());
        solicitud.setUbicacionExterna(request.getUbicacionExterna());
        solicitud.setResponsableEvento(request.getResponsableEvento());
        solicitud.setTipoEventoCatalogo(tipoEvento);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setOficina(oficina);
        solicitud.setUsuarioSolicitante(usuario);
        solicitud.setRequiereTransmision(request.getRequiereTransmision() != null ? request.getRequiereTransmision() : false);
        solicitud.setRequiereCubrimiento(request.getRequiereCubrimiento() != null ? request.getRequiereCubrimiento() : false);
        solicitud.setRequierePiezaGrafica(request.getRequierePiezaGrafica() != null ? request.getRequierePiezaGrafica() : false);
        solicitud.setRequiereServiciosGenerales(request.getRequiereServiciosGenerales() != null ? request.getRequiereServiciosGenerales() : false);
        solicitud.setObservaciones(request.getObservaciones());
        solicitud.setFrecuenciaRecurrencia(request.getFrecuenciaRecurrencia() != null ? request.getFrecuenciaRecurrencia() : com.calendario.callapp.callapp_backend.entity.FrecuenciaRecurrencia.NINGUNA);
        solicitud.setFechaFinRecurrencia(request.getFechaFinRecurrencia());

        if ((usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones()) && request.getEsImportante() != null) {
            solicitud.setEsImportante(request.getEsImportante());
        } else {
            solicitud.setEsImportante(false);
        }

        solicitud.setTipoIngreso(request.getTipoIngreso() != null
                ? request.getTipoIngreso()
                : com.calendario.callapp.callapp_backend.entity.TipoIngreso.LIBRE);
    }

    private boolean esEstadoEditable(EstadoSolicitud estado) {
        return estado == EstadoSolicitud.PENDIENTE
            || estado == EstadoSolicitud.RECHAZADA
            || estado == EstadoSolicitud.EN_REVISION;
    }

    public static List<LocalDate> calcularFechasRecurrencia(LocalDate inicio,
                                                     FrecuenciaRecurrencia frecuencia,
                                                     LocalDate fechaFin,
                                                     int maxInstancias) {
        List<LocalDate> fechas = new java.util.ArrayList<>();
        LocalDate temp = inicio;
        while (true) {
            switch (frecuencia) {
                case DIARIA -> temp = temp.plusDays(1);
                case SEMANAL -> temp = temp.plusWeeks(1);
                case MENSUAL -> temp = temp.plusMonths(1);
                default -> temp = temp.plusYears(100);
            }
            if (temp.isAfter(fechaFin)) break;
            fechas.add(temp);
            if (fechas.size() >= maxInstancias) break;
        }
        return fechas;
    }

    private void generarInstanciasDesde(SolicitudEvento principal,
                                        FrecuenciaRecurrencia frecuencia,
                                        LocalDate fechaFin,
                                        TipoEventoCatalogo tipoEvento,
                                        Oficina oficina,
                                        Usuario usuario,
                                        List<SolicitudEventoParticipanteRequest> participantes) {
        if (fechaFin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Para eventos recurrentes, la fecha de fin es obligatoria");
        }
        final int MAX_INSTANCIAS = 100;

        List<LocalDate> todasLasFechas = calcularFechasRecurrencia(
            principal.getFechaEvento(), frecuencia, fechaFin, MAX_INSTANCIAS);

        List<Long> lugarIds = principal.getLugaresFisicos().stream()
                .map(LugarFisico::getId).toList();
        List<SolicitudEvento> posiblesConflictos = new java.util.ArrayList<>();
        if (!todasLasFechas.isEmpty() && !lugarIds.isEmpty()) {
            posiblesConflictos = solicitudEventoRepository.findConflictsBulk(
                todasLasFechas, lugarIds, principal.getHoraInicio(), principal.getHoraFin(), principal.getId());
        }

        List<SolicitudEvento> nuevasInstancias = new java.util.ArrayList<>();
        for (LocalDate fecha : todasLasFechas) {
            for (SolicitudEvento conflicto : posiblesConflictos) {
                if (conflicto.getFechaEvento().equals(fecha)) {
                    boolean hayChoqueLugar = conflicto.getLugaresFisicos().stream()
                            .anyMatch(l -> lugarIds.contains(l.getId()));
                    if (hayChoqueLugar) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                            String.format("Conflicto en serie: El %s hay un choque con el evento '%s'",
                                fecha, conflicto.getNombreEvento()));
                    }
                }
            }

            SolicitudEvento instancia = new SolicitudEvento();
            instancia.setNombreEvento(principal.getNombreEvento());
            instancia.setDescripcionEvento(principal.getDescripcionEvento());
            instancia.setFechaEvento(fecha);
            instancia.setHoraInicio(principal.getHoraInicio());
            instancia.setHoraFin(principal.getHoraFin());
            instancia.getLugaresFisicos().clear();
            instancia.getLugaresFisicos().addAll(principal.getLugaresFisicos());
            instancia.setLinkConexion(principal.getLinkConexion());
            instancia.setUbicacionExterna(principal.getUbicacionExterna());
            instancia.setResponsableEvento(principal.getResponsableEvento());
            instancia.setTipoEventoCatalogo(tipoEvento);
            instancia.setEstado(EstadoSolicitud.PENDIENTE);
            instancia.setOficina(oficina);
            instancia.setUsuarioSolicitante(usuario);
            instancia.setRequiereTransmision(principal.getRequiereTransmision());
            instancia.setRequiereCubrimiento(principal.getRequiereCubrimiento());
            instancia.setRequierePiezaGrafica(principal.getRequierePiezaGrafica());
            instancia.setRequiereServiciosGenerales(principal.getRequiereServiciosGenerales());
            instancia.setObservaciones(principal.getObservaciones());
            instancia.setEsImportante(principal.getEsImportante());
            instancia.setTipoIngreso(principal.getTipoIngreso());
            instancia.setIdGrupoRecurrencia(principal.getIdGrupoRecurrencia());
            instancia.setEsPrincipal(false);
            instancia.setFrecuenciaRecurrencia(FrecuenciaRecurrencia.NINGUNA);

            guardarParticipantes(instancia, participantes);
            nuevasInstancias.add(instancia);
        }

        if (!nuevasInstancias.isEmpty()) {
            solicitudEventoRepository.saveAll(nuevasInstancias);
        }
    }

    private void reconciliarRecurrencia(SolicitudEvento principal,
                                        SolicitudEventoRequest request,
                                        TipoEventoCatalogo tipoEvento,
                                        Oficina oficina,
                                        Usuario usuario,
                                        boolean firmaCambio) {
        FrecuenciaRecurrencia nuevaFrec = request.getFrecuenciaRecurrencia() != null
                ? request.getFrecuenciaRecurrencia()
                : FrecuenciaRecurrencia.NINGUNA;
        LocalDate nuevaFechaFin = request.getFechaFinRecurrencia();
        boolean quiereRecurrente = nuevaFrec != FrecuenciaRecurrencia.NINGUNA && nuevaFechaFin != null;

        boolean eraRecurrente = principal.getIdGrupoRecurrencia() != null;

        if (!eraRecurrente && !quiereRecurrente) {
            principal.setFrecuenciaRecurrencia(FrecuenciaRecurrencia.NINGUNA);
            principal.setFechaFinRecurrencia(null);
            return;
        }

        if (eraRecurrente && quiereRecurrente && !firmaCambio) {
            principal.setFrecuenciaRecurrencia(nuevaFrec);
            principal.setFechaFinRecurrencia(nuevaFechaFin);
            return;
        }

        if (principal.getIdGrupoRecurrencia() != null) {
            List<SolicitudEvento> serie =
                solicitudEventoRepository.findAllByIdGrupoRecurrencia(principal.getIdGrupoRecurrencia());
            List<SolicitudEvento> secundariasEditables = serie.stream()
                .filter(s -> !java.util.Objects.equals(s.getId(), principal.getId()))
                .filter(s -> !Boolean.TRUE.equals(s.getEsPrincipal()))
                .filter(s -> esEstadoEditable(s.getEstado()))
                .toList();
            for (SolicitudEvento s : secundariasEditables) {
                publicacionEventoRepository.findBySolicitudEventoId(s.getId())
                    .ifPresent(publicacionEventoRepository::delete);
            }
            if (!secundariasEditables.isEmpty()) {
                solicitudEventoRepository.deleteAll(secundariasEditables);
                solicitudEventoRepository.flush();
            }
        }

        if (!quiereRecurrente) {
            principal.setFrecuenciaRecurrencia(FrecuenciaRecurrencia.NINGUNA);
            principal.setFechaFinRecurrencia(null);
            principal.setIdGrupoRecurrencia(null);
            principal.setEsPrincipal(false);
            return;
        }

        if (principal.getIdGrupoRecurrencia() == null) {
            principal.setIdGrupoRecurrencia(java.util.UUID.randomUUID().toString());
        }
        principal.setEsPrincipal(true);
        principal.setFrecuenciaRecurrencia(nuevaFrec);
        principal.setFechaFinRecurrencia(nuevaFechaFin);

        generarInstanciasDesde(principal, nuevaFrec, nuevaFechaFin,
            tipoEvento, oficina, usuario, request.getParticipantes());
    }

    @Transactional(readOnly = true)
    public List<SolicitudEventoResponse> listarPropias(
            Authentication authentication,
            String q,
            EstadoSolicitud estado,
            Integer mes,
            Integer anio) {
        Usuario usuario = obtenerUsuario(authentication);
        Oficina oficina = usuario.getOficina();
        
        if (oficina == null) {
             return List.of();
        }

        List<SolicitudEvento> lista = solicitudEventoRepository.getAllByOficinaIdOptimized(oficina.getId())
                .stream()
                .filter(solicitud -> coincideFiltroSolicitud(solicitud, q, estado, mes, anio))
                .toList();
        
        return enrichResponseList(solicitudEventoMapper.toResponseList(lista));
    }


    @Transactional(readOnly = true)
    public SolicitudEventoResponse obtenerPropia(Long id, Authentication authentication) {
        SolicitudEvento solicitud = buscarSolicitud(id);
        validarAccesoOficina(solicitud, authentication);
        return enrichResponse(solicitudEventoMapper.toResponse(solicitud));
    }


    @Transactional
    @SuppressWarnings("null")
    public SolicitudEventoResponse actualizarPropia(Long id, SolicitudEventoRequest request, Authentication authentication) {
        validarHorario(request);

        SolicitudEvento solicitud = buscarSolicitud(id);
        FrecuenciaRecurrencia frecAntes = solicitud.getFrecuenciaRecurrencia() != null
                ? solicitud.getFrecuenciaRecurrencia() : FrecuenciaRecurrencia.NINGUNA;
        LocalDate fechaFinAntes = solicitud.getFechaFinRecurrencia();
        LocalDate fechaInicioAntes = solicitud.getFechaEvento();
        Usuario usuario = obtenerUsuario(authentication);
        validarAccesoOficina(solicitud, authentication);

        // Lógica normal para instancia individual (incluyendo el maestro de la serie)
        boolean isAdmin = usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones();
        boolean isAprobada = solicitud.getEstado() == EstadoSolicitud.APROBADA;

        if (!isAdmin && solicitud.getEstado() == EstadoSolicitud.RECHAZADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Esta solicitud fue rechazada definitivamente y no se puede reenviar. Solo las solicitudes en revisión pueden editarse y reenviarse.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE &&
            solicitud.getEstado() != EstadoSolicitud.RECHAZADA &&
            solicitud.getEstado() != EstadoSolicitud.EN_REVISION &&
            !(isAdmin && isAprobada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo puedes editar solicitudes pendientes o rechazadas");
        }
        TipoEventoCatalogo tipoEvento = resolveTipoEvento(request.getTipoEvento());

        solicitud.setNombreEvento(request.getNombreEvento());
        solicitud.setDescripcionEvento(request.getDescripcionEvento());
        // La fecha SI se actualiza si es edición individual
        solicitud.setFechaEvento(request.getFechaEvento());
        solicitud.setHoraInicio(request.getHoraInicio());
        solicitud.setHoraFin(request.getHoraFin());
        if (request.getIdsLugaresFisicos() != null) {
            solicitud.getLugaresFisicos().clear();
            solicitud.getLugaresFisicos().addAll(lugarFisicoRepository.findAllById(request.getIdsLugaresFisicos()));
        }
        solicitud.setLinkConexion(request.getLinkConexion());
        solicitud.setUbicacionExterna(request.getUbicacionExterna());
        solicitud.setResponsableEvento(request.getResponsableEvento());
        solicitud.setTipoEventoCatalogo(tipoEvento);
        solicitud.setPiezaGraficaUrl(request.getPiezaGraficaUrl());

        if (request.getRequiereTransmision() != null) solicitud.setRequiereTransmision(request.getRequiereTransmision());
        if (request.getRequiereCubrimiento() != null) solicitud.setRequiereCubrimiento(request.getRequiereCubrimiento());
        if (request.getRequierePiezaGrafica() != null) solicitud.setRequierePiezaGrafica(request.getRequierePiezaGrafica());
        if (request.getRequiereServiciosGenerales() != null) solicitud.setRequiereServiciosGenerales(request.getRequiereServiciosGenerales());
        if (request.getObservaciones() != null) solicitud.setObservaciones(request.getObservaciones());

        if ((usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones()) && request.getEsImportante() != null) {
            solicitud.setEsImportante(request.getEsImportante());
        }

        if (request.getTipoIngreso() != null) solicitud.setTipoIngreso(request.getTipoIngreso());

        checkConflicts(solicitud.getFechaEvento(), solicitud.getLugaresFisicos(), solicitud.getHoraInicio(), solicitud.getHoraFin(), solicitud.getId());

        // Si es Admin y ya estaba aprobada, no resetear a PENDIENTE
        if (!(isAdmin && isAprobada)) {
            solicitud.setEstado(EstadoSolicitud.PENDIENTE);
            solicitud.setMotivoRechazo(null);
            solicitud.setObservacionesRevision(null);
            solicitud.setUsuarioRevisor(null);
            solicitud.setFechaRevision(null);
        }

        // Si es admin y envía idOficina, permitir cambiar la oficina de la solicitud
        if (usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones()) {
            if (request.getIdOficina() != null) {
                Oficina nuevaOficina = oficinaRepository.findById(request.getIdOficina())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oficina no encontrada"));
                solicitud.setOficina(nuevaOficina);
            }
        }

        boolean esSecundaria = solicitud.getIdGrupoRecurrencia() != null
                && !Boolean.TRUE.equals(solicitud.getEsPrincipal());
        if (!esSecundaria) {
            FrecuenciaRecurrencia nuevaFrec = request.getFrecuenciaRecurrencia() != null
                    ? request.getFrecuenciaRecurrencia() : FrecuenciaRecurrencia.NINGUNA;
            boolean firmaCambio = nuevaFrec != frecAntes
                    || !java.util.Objects.equals(request.getFechaFinRecurrencia(), fechaFinAntes)
                    || !java.util.Objects.equals(request.getFechaEvento(), fechaInicioAntes);
            reconciliarRecurrencia(solicitud, request, tipoEvento, solicitud.getOficina(), usuario, firmaCambio);
        }

        guardarParticipantes(solicitud, request.getParticipantes());
        SolicitudEvento guardada = solicitudEventoRepository.save(solicitud);

        return enrichResponse(solicitudEventoMapper.toResponse(guardada));
    }


    @Transactional
    public void eliminarPropia(Long id, Authentication authentication) {
        SolicitudEvento solicitud = buscarSolicitud(id);
        validarAccesoOficina(solicitud, authentication);

        if (solicitud.getEsPrincipal() && solicitud.getIdGrupoRecurrencia() != null) {
            eliminarSerie(solicitud.getIdGrupoRecurrencia(), authentication);
            return;
        }

        // Si está publicada, eliminar la publicación primero
        publicacionEventoRepository.findBySolicitudEventoId(id)
                .ifPresent(publicacionEventoRepository::delete);

        solicitudEventoRepository.delete(solicitud);
    }

    @Transactional(readOnly = true)
    public List<SolicitudEventoResponse> listarParaRevision(String q, EstadoSolicitud estado, Integer mes, Integer anio) {
        return enrichResponseList(solicitudEventoMapper.toResponseList(
            solicitudEventoRepository.getAllUniqueWithAssociations(PageRequest.of(0, 1000)).stream()
                .filter(solicitud -> coincideFiltroSolicitud(solicitud, q, estado, mes, anio))
                .toList()
        ));
    }

    @Transactional(readOnly = true)
    public SolicitudEventoResponse obtenerParaRevision(Long id) {
        return enrichResponse(solicitudEventoMapper.toResponse(buscarSolicitud(id)));
    }


    @Transactional
    public SolicitudEventoResponse aprobar(Long id, Authentication authentication) {
        SolicitudEvento solicitud = buscarSolicitud(id);

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE &&
            solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No se puede aprobar una solicitud en estado " + solicitud.getEstado().name());
        }

        Usuario revisor = obtenerUsuario(authentication);

        // Para series, aprobarSerie hace su propio checkConflicts por cada instancia
        if (solicitud.getEsPrincipal() && solicitud.getIdGrupoRecurrencia() != null) {
            aprobarSerie(solicitud.getIdGrupoRecurrencia(), authentication);
            return enrichResponse(solicitudEventoMapper.toResponse(solicitud));
        }

        checkConflicts(solicitud.getFechaEvento(), solicitud.getLugaresFisicos(),
                solicitud.getHoraInicio(), solicitud.getHoraFin(), solicitud.getId());

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setMotivoRechazo(null);
        solicitud.setUsuarioRevisor(revisor);
        solicitud.setFechaRevision(LocalDateTime.now());

        SolicitudEvento guardada = solicitudEventoRepository.save(solicitud);
        
        notificacionService.notificarAprobacionEvento(
            guardada.getNombreEvento(),
            guardada.getUsuarioSolicitante() != null ? guardada.getUsuarioSolicitante().getCorreo() : null
        );

        return enrichResponse(solicitudEventoMapper.toResponse(guardada));
    }

    @Transactional
    public SolicitudEventoResponse rechazar(Long id, RechazoRequest request, Authentication authentication) {
        SolicitudEvento solicitud = buscarSolicitud(id);

        if (solicitud.getEsPrincipal() && solicitud.getIdGrupoRecurrencia() != null) {
            rechazarSerie(solicitud.getIdGrupoRecurrencia(), request, authentication);
            return enrichResponse(solicitudEventoMapper.toResponse(solicitud));
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE &&
            solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No se puede rechazar una solicitud en estado " + solicitud.getEstado().name());
        }

        Usuario revisor = obtenerUsuario(authentication);
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setMotivoRechazo(request.getMotivo());
        solicitud.setUsuarioRevisor(revisor);
        solicitud.setFechaRevision(LocalDateTime.now());

        // Defensivo: si el evento tenía una publicación visible, ocultarla al rechazar
        publicacionEventoRepository.findBySolicitudEventoId(solicitud.getId())
            .ifPresent(pub -> {
                pub.setVisible(false);
                publicacionEventoRepository.save(pub);
            });

        SolicitudEvento guardada = solicitudEventoRepository.save(solicitud);
        
        notificacionService.notificarRechazoEvento(
            guardada.getNombreEvento(),
            guardada.getMotivoRechazo(),
            guardada.getUsuarioSolicitante() != null ? guardada.getUsuarioSolicitante().getCorreo() : null
        );

        return solicitudEventoMapper.toResponse(guardada);
    }

    @Transactional
    public SolicitudEventoResponse devolver(Long id, DevolucionRequest request, Authentication authentication) {
        SolicitudEvento solicitud = buscarSolicitud(id);
        Usuario revisor = obtenerUsuario(authentication);
        LocalDateTime ahora = LocalDateTime.now();

        // Para el maestro de una serie, propagar EN_REVISION a todas las instancias
        if (solicitud.getEsPrincipal() && solicitud.getIdGrupoRecurrencia() != null) {
            List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrencia(solicitud.getIdGrupoRecurrencia());
            for (SolicitudEvento s : serie) {
                s.setEstado(EstadoSolicitud.EN_REVISION);
                s.setObservacionesRevision(request.getObservaciones());
                s.setUsuarioRevisor(revisor);
                s.setFechaRevision(ahora);
            }
            solicitudEventoRepository.saveAll(serie);
            return enrichResponse(solicitudEventoMapper.toResponse(solicitud));
        }

        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
        solicitud.setObservacionesRevision(request.getObservaciones());
        solicitud.setUsuarioRevisor(revisor);
        solicitud.setFechaRevision(ahora);

        SolicitudEvento guardada = solicitudEventoRepository.save(solicitud);
        return solicitudEventoMapper.toResponse(guardada);
    }

    @Transactional
    @SuppressWarnings("null")
    public void aprobarSerie(String idGrupo, Authentication authentication) {
        if (idGrupo == null || idGrupo.isBlank()) return;
        // JOIN FETCH lugaresFisicos para evitar N+1 en checkConflicts dentro del loop
        List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrenciaConLugares(idGrupo);
        Usuario revisor = obtenerUsuario(authentication);
        LocalDateTime ahora = LocalDateTime.now();

        for (SolicitudEvento s : serie) {
            if (s.getEstado() == EstadoSolicitud.PENDIENTE || s.getEstado() == EstadoSolicitud.RECHAZADA) {
                checkConflicts(s.getFechaEvento(), s.getLugaresFisicos(),
                        s.getHoraInicio(), s.getHoraFin(), s.getId());
                s.setEstado(EstadoSolicitud.APROBADA);
                s.setMotivoRechazo(null);
                s.setUsuarioRevisor(revisor);
                s.setFechaRevision(ahora);
            }
        }
        solicitudEventoRepository.saveAll(serie);

        if (!serie.isEmpty()) {
            SolicitudEvento principal = serie.stream().filter(SolicitudEvento::getEsPrincipal).findFirst().orElse(serie.get(0));
            notificacionService.notificarAprobacionEvento(
                principal.getNombreEvento() + " (Serie Completa)",
                principal.getUsuarioSolicitante() != null ? principal.getUsuarioSolicitante().getCorreo() : null
            );
        }
    }

    @Transactional
    public PublicacionEventoResponse publicar(Long id, PublicacionEventoRequest request, Authentication authentication) {
        SolicitudEvento solicitud = buscarSolicitud(id);

        if (solicitud.getEstado() != EstadoSolicitud.APROBADA && solicitud.getEstado() != EstadoSolicitud.PUBLICADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo puedes publicar una solicitud aprobada");
        }

        Usuario publicador = obtenerUsuario(authentication);
        PublicacionEvento publicacion = publicacionEventoRepository.findBySolicitudEventoId(id).orElseGet(PublicacionEvento::new);
        publicacion.setSolicitudEvento(solicitud);
        publicacion.setTituloVisible(request.getTituloVisible());
        publicacion.setDescripcionVisible(request.getDescripcionVisible());
        publicacion.setPiezaGraficaUrl(request.getPiezaGraficaUrl());
        publicacion.setFechaPublicacion(request.getFechaPublicacion() != null ? request.getFechaPublicacion() : LocalDateTime.now());
        publicacion.setVisible(true);
        publicacion.setUsuarioPublicador(publicador);

        solicitud.setEstado(EstadoSolicitud.PUBLICADA);
        if (request.getPiezaGraficaUrl() != null && !request.getPiezaGraficaUrl().isEmpty()) {
            solicitud.setPiezaGraficaUrl(request.getPiezaGraficaUrl());
        }
        solicitudEventoRepository.save(solicitud);

        PublicacionEvento guardada = publicacionEventoRepository.save(publicacion);
        
        notificacionService.notificarPublicacionEvento(
            guardada.getId(),
            guardada.getTituloVisible(),
            solicitud.getDescripcionEvento(),
            solicitud.getFechaEvento() != null ? solicitud.getFechaEvento().toString() : "",
            solicitud.getHoraInicio() != null ? solicitud.getHoraInicio().toString() : "",
            solicitud.getHoraFin() != null ? solicitud.getHoraFin().toString() : "",
            solicitud.getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).collect(java.util.stream.Collectors.joining(", ")),
            solicitud.getTipoEventoCatalogo() != null ? solicitud.getTipoEventoCatalogo().getNombre() : "N/A",
            solicitud.getResponsableEvento(),
            solicitud.getOficina() != null ? solicitud.getOficina().getNombre() : "",
            guardada.getPiezaGraficaUrl(),
            solicitud.getUsuarioSolicitante() != null ? solicitud.getUsuarioSolicitante().getCorreo() : null
        );

        triggerImportantEventPush(guardada);
        return publicacionEventoMapper.toResponse(guardada);
    }

    @Transactional
    @SuppressWarnings("null")
    public void rechazarSerie(String idGrupo, RechazoRequest request, Authentication authentication) {
        if (idGrupo == null || idGrupo.isBlank()) return;
        List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrencia(idGrupo);
        Usuario revisor = obtenerUsuario(authentication);
        LocalDateTime ahora = LocalDateTime.now();

        for (SolicitudEvento s : serie) {
            s.setEstado(EstadoSolicitud.RECHAZADA);
            s.setMotivoRechazo(request.getMotivo());
            s.setUsuarioRevisor(revisor);
            s.setFechaRevision(ahora);
        }
        solicitudEventoRepository.saveAll(serie);

        if (!serie.isEmpty()) {
            SolicitudEvento principal = serie.stream()
                .filter(SolicitudEvento::getEsPrincipal)
                .findFirst()
                .orElse(serie.get(0));
            
            notificacionService.notificarRechazoEvento(
                principal.getNombreEvento() + " (Serie Completa)",
                principal.getMotivoRechazo(),
                principal.getUsuarioSolicitante() != null ? principal.getUsuarioSolicitante().getCorreo() : null
            );
        }
    }

    @Transactional
    @SuppressWarnings("null")
    public void publicarSerie(String idGrupo, PublicacionEventoRequest request, Authentication authentication) {
        if (idGrupo == null || idGrupo.isBlank()) return;
        // JOIN FETCH lugaresFisicos para evitar lazy load en la notificación
        List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrenciaConLugares(idGrupo);
        Usuario publicador = obtenerUsuario(authentication);
        LocalDateTime ahora = LocalDateTime.now();

        PublicacionEvento maestraGuardada = null;
        for (SolicitudEvento s : serie) {
            // Solo publicar si está aprobada o ya publicada (para actualizar datos)
            if (s.getEstado() == EstadoSolicitud.APROBADA || s.getEstado() == EstadoSolicitud.PUBLICADA) {
                PublicacionEvento p = publicacionEventoRepository.findBySolicitudEventoId(s.getId()).orElseGet(PublicacionEvento::new);
                p.setSolicitudEvento(s);
                p.setTituloVisible(request.getTituloVisible());
                p.setDescripcionVisible(request.getDescripcionVisible());
                p.setPiezaGraficaUrl(request.getPiezaGraficaUrl());
                p.setFechaPublicacion(request.getFechaPublicacion() != null ? request.getFechaPublicacion() : ahora);
                p.setVisible(true);
                p.setUsuarioPublicador(publicador);

                s.setEstado(EstadoSolicitud.PUBLICADA);
                if (request.getPiezaGraficaUrl() != null && !request.getPiezaGraficaUrl().isEmpty()) {
                    s.setPiezaGraficaUrl(request.getPiezaGraficaUrl());
                }
                PublicacionEvento guardada = publicacionEventoRepository.save(p);
                if (s.getEsPrincipal()) {
                    maestraGuardada = guardada;
                }
            }
        }
        
        // Notificar UNA SOLA VEZ para toda la serie
        if (maestraGuardada != null) {
            SolicitudEvento s = maestraGuardada.getSolicitudEvento();
            notificacionService.notificarPublicacionEvento(
                maestraGuardada.getId(),
                maestraGuardada.getTituloVisible() + " (Serie de eventos)",
                s.getDescripcionEvento(),
                s.getFechaEvento() != null ? s.getFechaEvento().toString() : "",
                s.getHoraInicio() != null ? s.getHoraInicio().toString() : "",
                s.getHoraFin() != null ? s.getHoraFin().toString() : "",
                s.getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).collect(java.util.stream.Collectors.joining(", ")),
                s.getTipoEventoCatalogo() != null ? s.getTipoEventoCatalogo().getNombre() : "N/A",
                s.getResponsableEvento(),
                s.getOficina() != null ? s.getOficina().getNombre() : "",
                maestraGuardada.getPiezaGraficaUrl(),
                s.getUsuarioSolicitante() != null ? s.getUsuarioSolicitante().getCorreo() : null
            );
        }
        solicitudEventoRepository.saveAll(serie);
    }

    @Transactional
    @SuppressWarnings("null")
    public void eliminarSerie(String idGrupo, Authentication authentication) {
        if (idGrupo == null || idGrupo.isBlank()) return;
        List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrencia(idGrupo);
        if (serie.isEmpty()) return;

        SolicitudEvento principal = serie.stream()
            .filter(SolicitudEvento::getEsPrincipal)
            .findFirst()
            .orElse(serie.get(0));
        validarAccesoOficina(principal, authentication);

        // Solo el creador de la serie o un administrador puede eliminarla
        Usuario solicitante = obtenerUsuario(authentication);
        boolean isAdmin = solicitante.getRol().esAdministradorGlobal() || solicitante.getRol().esComunicaciones();
        if (!isAdmin && (principal.getUsuarioSolicitante() == null
                || !principal.getUsuarioSolicitante().getId().equals(solicitante.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes eliminar series que tú creaste");
        }

        // Borrado en lote: una sola query para publicaciones, deleteAll para solicitudes (cascade de participantes)
        List<Long> ids = serie.stream().map(SolicitudEvento::getId).toList();
        publicacionEventoRepository.deleteBySolicitudEventoIdIn(ids);
        solicitudEventoRepository.deleteAll(serie);
    }

    @Transactional(readOnly = true)
    public List<PublicacionEventoResponse> listarPublicadas(
            String filtro,
            LocalDate fecha,
            String q,
            String tipoEvento,
            Integer mes,
            Integer anio) {
        List<PublicacionEvento> publicaciones;
        if (filtro == null || filtro.isBlank()) {
            publicaciones = publicacionEventoRepository.getAllVisibleOptimized(PageRequest.of(0, 300));
        } else {
            LocalDate fechaBase = fecha != null ? fecha : LocalDate.now();
            LocalDate inicio = calcularInicio(filtro, fechaBase);
            LocalDate fin = calcularFin(filtro, fechaBase);
            publicaciones = publicacionEventoRepository.findPublicadasEnRango(inicio, fin, PageRequest.of(0, 500));
        }

        return publicacionEventoMapper.toResponseList(
            publicaciones.stream()
                .filter(publicacion -> coincideFiltroPublicacion(publicacion, q, tipoEvento, mes, anio))
                .toList()
        );
    }

    @Transactional(readOnly = true)
    public List<PublicacionEventoResponse> listarProximos(int limit, String q) {
        int limiteSeguro = Math.max(1, Math.min(limit, 50));
        LocalDate hoy = LocalDate.now();

        // Si hay búsqueda 'q', aún dependemos de filtrado en memoria por ahora o expandir Repo
        if (q != null && !q.isBlank()) {
            return publicacionEventoRepository.getAllVisibleOptimized(PageRequest.of(0, 300)).stream()
                    .filter(publicacion -> !publicacion.getSolicitudEvento().getFechaEvento().isBefore(hoy))
                    .filter(publicacion -> coincideFiltroPublicacion(publicacion, q, null, null, null))
                    .sorted((a, b) -> {
                        int fecha = a.getSolicitudEvento().getFechaEvento().compareTo(b.getSolicitudEvento().getFechaEvento());
                        if (fecha != 0) return fecha;
                        return a.getSolicitudEvento().getHoraInicio().compareTo(b.getSolicitudEvento().getHoraInicio());
                    })
                    .limit(limiteSeguro)
                    .map(publicacionEventoMapper::toResponse)
                    .toList();
        }

        // Caso optimizado sin búsqueda: Consulta directa a BD con límite y orden
        return publicacionEventoMapper.toResponseList(
            publicacionEventoRepository.findProximos(hoy, PageRequest.of(0, limiteSeguro))
        );
    }

    @Transactional(readOnly = true)
    public PublicacionEventoResponse obtenerPublicacion(Long id) {
        PublicacionEvento publicacion = publicacionEventoRepository.getByIdAndVisibleUnique(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicacion de evento no encontrada"));
        return publicacionEventoMapper.toResponse(publicacion);
    }

    @Transactional
    public PublicacionEventoResponse toggleVisibilidad(Long id, boolean visible) {
        SolicitudEvento solicitud = buscarSolicitud(id);

        if (solicitud.getEsPrincipal() && solicitud.getIdGrupoRecurrencia() != null) {
            // Validar que el maestro esté publicado ANTES de propagar para evitar actualización parcial
            PublicacionEvento pub = publicacionEventoRepository.findBySolicitudEventoId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El evento maestro de la serie no ha sido publicado"));
            toggleVisibilidadSerie(solicitud.getIdGrupoRecurrencia(), visible);
            return publicacionEventoMapper.toResponse(pub);
        }

        PublicacionEvento publicacion = publicacionEventoRepository.findBySolicitudEventoId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El evento aun no ha sido publicado oficialmente"));
        
        publicacion.setVisible(visible);
        return publicacionEventoMapper.toResponse(publicacionEventoRepository.save(publicacion));
    }

    @Transactional
    public void toggleVisibilidadSerie(String idGrupo, boolean visible) {
        if (idGrupo == null || idGrupo.isBlank()) return;
        List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrencia(idGrupo);
        List<Long> ids = serie.stream().map(SolicitudEvento::getId).toList();
        List<PublicacionEvento> publicaciones = publicacionEventoRepository.findBySolicitudEventoIdIn(ids);
        publicaciones.forEach(p -> p.setVisible(visible));
        publicacionEventoRepository.saveAll(publicaciones);
    }

    @Transactional
    public void eliminarPublicacion(Long id) {
        PublicacionEvento publicacion = publicacionEventoRepository.findBySolicitudEventoId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada para el evento: " + id));
        
        SolicitudEvento solicitud = publicacion.getSolicitudEvento();
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitudEventoRepository.save(solicitud);
        
        publicacionEventoRepository.delete(publicacion);
    }

    @Transactional
    @SuppressWarnings("null")
    public PublicacionEventoResponse updatePublicacion(Long id, UpdatePublicacionRequest request) {
        PublicacionEvento publicacion = publicacionEventoRepository.findBySolicitudEventoId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));

        // Obtener la solicitud subyacente primero para usarla en los setters
        SolicitudEvento solicitud = publicacion.getSolicitudEvento();

        if (request.getTituloVisible() != null) publicacion.setTituloVisible(request.getTituloVisible());
        if (request.getDescripcionVisible() != null) publicacion.setDescripcionVisible(request.getDescripcionVisible());
        publicacion.setPiezaGraficaUrl(request.getPiezaGraficaUrl());
        solicitud.setPiezaGraficaUrl(request.getPiezaGraficaUrl());

        // Continuar actualizando solicitud subyacente
        if (request.getNombreEvento() != null) solicitud.setNombreEvento(request.getNombreEvento());
        if (request.getDescripcionEvento() != null) solicitud.setDescripcionEvento(request.getDescripcionEvento());
        if (request.getFechaEvento() != null) solicitud.setFechaEvento(request.getFechaEvento());
        if (request.getHoraInicio() != null) solicitud.setHoraInicio(request.getHoraInicio());
        if (request.getHoraFin() != null) solicitud.setHoraFin(request.getHoraFin());
        if (request.getIdsLugaresFisicos() != null) {
            solicitud.getLugaresFisicos().clear();
            solicitud.getLugaresFisicos().addAll(lugarFisicoRepository.findAllById(request.getIdsLugaresFisicos()));
        }
        if (request.getLinkConexion() != null) solicitud.setLinkConexion(request.getLinkConexion());
        if (request.getUbicacionExterna() != null) solicitud.setUbicacionExterna(request.getUbicacionExterna());
        if (request.getResponsableEvento() != null) solicitud.setResponsableEvento(request.getResponsableEvento());
        if (request.getTipoEvento() != null) {
            TipoEventoCatalogo tipo = resolveTipoEvento(request.getTipoEvento());
            solicitud.setTipoEventoCatalogo(tipo);
        }
        
        if (request.getIdOficina() != null) {
            Oficina ofi = oficinaRepository.findById(request.getIdOficina())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oficina no encontrada"));
            solicitud.setOficina(ofi);
        }

        if (request.getRequiereTransmision() != null) solicitud.setRequiereTransmision(request.getRequiereTransmision());
        if (request.getRequiereCubrimiento() != null) solicitud.setRequiereCubrimiento(request.getRequiereCubrimiento());
        if (request.getRequierePiezaGrafica() != null) solicitud.setRequierePiezaGrafica(request.getRequierePiezaGrafica());
        if (request.getRequiereServiciosGenerales() != null) solicitud.setRequiereServiciosGenerales(request.getRequiereServiciosGenerales());
        if (request.getObservaciones() != null) solicitud.setObservaciones(request.getObservaciones());
        if (request.getEsImportante() != null) solicitud.setEsImportante(request.getEsImportante());
        if (request.getTipoIngreso() != null) solicitud.setTipoIngreso(request.getTipoIngreso());
        if (request.getParticipantes() != null) {
            guardarParticipantes(solicitud, request.getParticipantes());
        }

        checkConflicts(solicitud.getFechaEvento(), solicitud.getLugaresFisicos(), solicitud.getHoraInicio(), solicitud.getHoraFin(), solicitud.getId());

        solicitudEventoRepository.save(Objects.requireNonNull(solicitud));
        PublicacionEvento guardada = publicacionEventoRepository.save(publicacion);

        return publicacionEventoMapper.toResponse(guardada);
    }

    private void guardarParticipantes(SolicitudEvento solicitud, List<SolicitudEventoParticipanteRequest> participantes) {
        // orphanRemoval = true en la entidad maneja el borrado; no llamar al repo directamente
        // para evitar conflictos de estado en el contexto de persistencia de Hibernate
        solicitud.getParticipantes().clear();
        if (participantes == null) return;
        
        for (SolicitudEventoParticipanteRequest participanteRequest : participantes) {
            SolicitudEventoParticipante participante = new SolicitudEventoParticipante();
            participante.setSolicitudEvento(solicitud);
            participante.setNombre(participanteRequest.getNombre());
            participante.setCargo(participanteRequest.getCargo());
            participante.setDescripcion(participanteRequest.getDescripcion());
            participante.setFotoUrl(participanteRequest.getFotoUrl());
            participante.setTelefono(participanteRequest.getTelefono());
            participante.setCorreo(participanteRequest.getCorreo());
            participante.setTipo(participanteRequest.getTipo());
            
            solicitud.getParticipantes().add(participante);
        }
    }

    private void validarHorario(SolicitudEventoRequest request) {
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora fin debe ser estrictamente mayor que la hora inicio");
        }
    }

    private void checkConflicts(LocalDate fecha, List<LugarFisico> lugares, java.time.LocalTime inicio, java.time.LocalTime fin, Long excludeId) {
        if (lugares == null || lugares.isEmpty()) return;
        
        List<Long> lugarIds = lugares.stream().map(LugarFisico::getId).toList();
        List<SolicitudEvento> conflicts = solicitudEventoRepository.findConflictsBulk(java.util.List.of(fecha), lugarIds, inicio, fin, excludeId);
        
        if (!conflicts.isEmpty()) {
            SolicitudEvento c = conflicts.get(0);
            LugarFisico lugarConflicto = c.getLugaresFisicos().stream().filter(l -> lugarIds.contains(l.getId())).findFirst().orElse(lugares.get(0));
            
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                String.format("Conflicto de agenda: El lugar '%s' ya está reservado por el evento '%s' de %s a %s", 
                lugarConflicto.getNombre(), c.getNombreEvento(), c.getHoraInicio(), c.getHoraFin()));
        }
    }

    private SolicitudEvento buscarSolicitud(Long id) {
        return solicitudEventoRepository.getByIdOptimized(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud de evento no encontrada"));
    }

    private TipoEventoCatalogo resolveTipoEvento(String nombreTipoEvento) {
        return tipoEventoCatalogoRepository.findByNombreIgnoreCaseAndActivoTrue(nombreTipoEvento)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El tipo de evento enviado no existe o no esta activo"
                ));
    }

    @Transactional
    public SolicitudEventoResponse actualizarSerie(String idGrupo, SolicitudEventoRequest request, Authentication authentication) {
        if (idGrupo == null || idGrupo.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de grupo no proporcionado");
        
        validarHorario(request);

        List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrenciaConParticipantes(idGrupo);
        // Segunda query (misma sesión JPA) para inicializar lugaresFisicos en las mismas instancias del L1 cache.
        // Evita MultipleBagFetchException y N+1 al llamar getLugaresFisicos().clear() en el loop.
        solicitudEventoRepository.findAllByIdGrupoRecurrenciaConLugares(idGrupo);
        SolicitudEvento principal = serie.stream().filter(SolicitudEvento::getEsPrincipal).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la instancia maestra de la serie"));

        validarAccesoOficina(principal, authentication);
        Usuario usuario = obtenerUsuario(authentication);
        boolean isAdmin = usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones();

        if (!isAdmin && principal.getEstado() == EstadoSolicitud.RECHAZADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Esta serie fue rechazada definitivamente y no se puede reenviar. Solo las solicitudes en revisión pueden editarse y reenviarse.");
        }

        // Validar que la serie esté en un estado editable (usando el maestro como proxy)
        if (principal.getEstado() != EstadoSolicitud.PENDIENTE &&
            principal.getEstado() != EstadoSolicitud.RECHAZADA &&
            principal.getEstado() != EstadoSolicitud.EN_REVISION &&
            !(isAdmin && principal.getEstado() == EstadoSolicitud.APROBADA) &&
            !(isAdmin && principal.getEstado() == EstadoSolicitud.PUBLICADA)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No puedes editar una serie en estado " + principal.getEstado().name());
        }

        TipoEventoCatalogo tipoEvento = resolveTipoEvento(request.getTipoEvento());

        FrecuenciaRecurrencia frecAntesSerie = principal.getFrecuenciaRecurrencia() != null
                ? principal.getFrecuenciaRecurrencia() : FrecuenciaRecurrencia.NINGUNA;
        LocalDate fechaFinAntesSerie = principal.getFechaFinRecurrencia();
        LocalDate fechaInicioAntesSerie = principal.getFechaEvento();

        List<LugarFisico> lugaresSerie = null;
        if (request.getIdsLugaresFisicos() != null) {
            lugaresSerie = lugarFisicoRepository.findAllById(Objects.requireNonNull(request.getIdsLugaresFisicos()));
        }

        // Vectorizar checkConflicts: una sola query de conflictos para toda la serie
        // (hasta 100 instancias) en vez de una por instancia. Todas comparten el mismo
        // horario/lugares del request (solo la fecha difiere por instancia), así que se
        // consulta con la lista completa de fechas efectivas y se filtran en memoria los
        // "conflictos" que en realidad son otros miembros de la propia serie.
        List<LugarFisico> lugaresParaConflicto = lugaresSerie != null
                ? lugaresSerie
                : (!serie.isEmpty() ? serie.get(0).getLugaresFisicos() : List.<LugarFisico>of());
        List<Long> lugarIdsSerie = lugaresParaConflicto.stream().map(LugarFisico::getId).toList();
        Set<Long> idsPropiaSerie = serie.stream().map(SolicitudEvento::getId).collect(Collectors.toSet());
        List<SolicitudEvento> posiblesConflictosSerie = List.of();
        if (!lugarIdsSerie.isEmpty()) {
            List<LocalDate> fechasEfectivas = serie.stream()
                    .map(s -> s.getEsPrincipal() ? request.getFechaEvento() : s.getFechaEvento())
                    .toList();
            posiblesConflictosSerie = solicitudEventoRepository.findConflictsBulk(
                    fechasEfectivas, lugarIdsSerie, request.getHoraInicio(), request.getHoraFin(), null);
        }

        for (SolicitudEvento s : serie) {
            // Propagar campos de información general
            s.setNombreEvento(request.getNombreEvento());
            s.setDescripcionEvento(request.getDescripcionEvento());
            if (lugaresSerie != null) {
                s.getLugaresFisicos().clear();
                s.getLugaresFisicos().addAll(lugaresSerie);
            }
            s.setLinkConexion(request.getLinkConexion());
            s.setUbicacionExterna(request.getUbicacionExterna());
            s.setResponsableEvento(request.getResponsableEvento());
            s.setTipoEventoCatalogo(tipoEvento);
            s.setPiezaGraficaUrl(request.getPiezaGraficaUrl());
            s.setHoraInicio(request.getHoraInicio());
            s.setHoraFin(request.getHoraFin());

            // NO actualizamos la fecha de las instancias secundarias para no romper la recurrencia
            if (s.getEsPrincipal()) {
                s.setFechaEvento(request.getFechaEvento());
            }

            // Validar conflictos de lugar/hora con otros eventos para esta instancia,
            // usando la consulta batch precomputada arriba (no dispara query nueva).
            for (SolicitudEvento conflicto : posiblesConflictosSerie) {
                if (idsPropiaSerie.contains(conflicto.getId())) continue; // es la propia serie, no es un conflicto real
                if (!conflicto.getFechaEvento().equals(s.getFechaEvento())) continue;
                LugarFisico lugarConflicto = conflicto.getLugaresFisicos().stream()
                        .filter(l -> lugarIdsSerie.contains(l.getId()))
                        .findFirst()
                        .orElse(lugaresParaConflicto.isEmpty() ? null : lugaresParaConflicto.get(0));
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format("Conflicto de agenda: El lugar '%s' ya está reservado por el evento '%s' de %s a %s",
                                lugarConflicto != null ? lugarConflicto.getNombre() : "desconocido",
                                conflicto.getNombreEvento(), conflicto.getHoraInicio(), conflicto.getHoraFin()));
            }

            if (request.getRequiereTransmision() != null) s.setRequiereTransmision(request.getRequiereTransmision());
            if (request.getRequiereCubrimiento() != null) s.setRequiereCubrimiento(request.getRequiereCubrimiento());
            if (request.getRequierePiezaGrafica() != null) s.setRequierePiezaGrafica(request.getRequierePiezaGrafica());
            if (request.getRequiereServiciosGenerales() != null) s.setRequiereServiciosGenerales(request.getRequiereServiciosGenerales());
            if (request.getObservaciones() != null) s.setObservaciones(request.getObservaciones());

            if (isAdmin && request.getEsImportante() != null) {
                s.setEsImportante(request.getEsImportante());
            }

            if (request.getTipoIngreso() != null) s.setTipoIngreso(request.getTipoIngreso());

            // Resetear estado a PENDIENTE para que vuelva al flujo de revisión
            // Excepción: admin editando una serie ya aprobada o publicada mantiene el estado
            if (!(isAdmin && (s.getEstado() == EstadoSolicitud.APROBADA || s.getEstado() == EstadoSolicitud.PUBLICADA))) {
                s.setEstado(EstadoSolicitud.PENDIENTE);
                s.setMotivoRechazo(null);
                s.setObservacionesRevision(null);
                s.setUsuarioRevisor(null);
                s.setFechaRevision(null);
            }

            // Propagar participantes a toda la serie
            guardarParticipantes(s, request.getParticipantes());
        }
        
        solicitudEventoRepository.saveAll(serie);

        // Reconciliar recurrencia solo si el request especifica explícitamente la frecuencia.
        // null = "no cambiar la estructura de recurrencia existente".
        if (request.getFrecuenciaRecurrencia() != null) {
            FrecuenciaRecurrencia nuevaFrecSerie = request.getFrecuenciaRecurrencia();
            boolean firmaCambioSerie = nuevaFrecSerie != frecAntesSerie
                    || !java.util.Objects.equals(request.getFechaFinRecurrencia(), fechaFinAntesSerie)
                    || !java.util.Objects.equals(request.getFechaEvento(), fechaInicioAntesSerie);
            if (firmaCambioSerie) {
                reconciliarRecurrencia(principal, request, tipoEvento, principal.getOficina(), usuario, true);
                solicitudEventoRepository.save(principal);
            }
        }

        // Actualizar publicaciones asociadas (para eventos ya publicados: sincroniza título, descripción y pieza gráfica)
        List<Long> idsEnSerie = serie.stream().map(SolicitudEvento::getId).toList();
        List<PublicacionEvento> publicaciones = publicacionEventoRepository.findBySolicitudEventoIdIn(idsEnSerie);
        if (!publicaciones.isEmpty()) {
            for (PublicacionEvento pub : publicaciones) {
                pub.setTituloVisible(request.getNombreEvento());
                if (request.getDescripcionEvento() != null) pub.setDescripcionVisible(request.getDescripcionEvento());
                if (request.getPiezaGraficaUrl() != null) pub.setPiezaGraficaUrl(request.getPiezaGraficaUrl());
            }
            publicacionEventoRepository.saveAll(publicaciones);
        }

        return enrichResponse(solicitudEventoMapper.toResponse(principal));
    }

    private Usuario obtenerUsuario(Authentication authentication) {
        return usuarioRepository.getByCorreoOptimized(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));
    }

    private void validarAccesoOficina(SolicitudEvento solicitud, Authentication authentication) {
        Usuario usuario = obtenerUsuario(authentication);
        if (!usuario.getRol().esAdministradorGlobal()
                && !usuario.getRol().esComunicaciones()
                && (usuario.getOficina() == null || !usuario.getOficina().getId().equals(solicitud.getOficina().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes acceder a esta solicitud");
        }
    }

    private LocalDate calcularInicio(String filtro, LocalDate fechaBase) {
        return switch (filtro.toLowerCase()) {
            case "dia" -> fechaBase;
            case "semana" -> fechaBase.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "mes" -> fechaBase.withDayOfMonth(1);
            case "anio" -> fechaBase.withDayOfYear(1);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro no valido. Usa: dia, semana, mes o anio");
        };
    }

    private LocalDate calcularFin(String filtro, LocalDate fechaBase) {
        return switch (filtro.toLowerCase()) {
            case "dia" -> fechaBase.plusDays(1);
            case "semana" -> fechaBase.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            case "mes" -> fechaBase.withDayOfMonth(1).plusMonths(1);
            case "anio" -> fechaBase.withDayOfYear(1).plusYears(1);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro no valido. Usa: dia, semana, mes o anio");
        };
    }

    private boolean coincideFiltroSolicitud(
            SolicitudEvento solicitud,
            String q,
            EstadoSolicitud estado,
            Integer mes,
            Integer anio) {
        if (estado != null && solicitud.getEstado() != estado) {
            return false;
        }
        if (mes != null && solicitud.getFechaEvento().getMonthValue() != mes) {
            return false;
        }
        if (anio != null && solicitud.getFechaEvento().getYear() != anio) {
            return false;
        }
        if (q == null || q.isBlank()) {
            return true;
        }

        String criterio = q.toLowerCase(Locale.ROOT).trim();
        return contiene(solicitud.getNombreEvento(), criterio)
                || contiene(solicitud.getDescripcionEvento(), criterio)
                || solicitud.getLugaresFisicos().stream().anyMatch(l -> contiene(l.getNombre(), criterio))
                || contiene(solicitud.getResponsableEvento(), criterio)
                || contiene(solicitud.getTipoEventoCatalogo() != null ? solicitud.getTipoEventoCatalogo().getNombre() : null, criterio)
                || (solicitud.getOficina() != null && contiene(solicitud.getOficina().getNombre(), criterio))
                || contiene(solicitud.getEstado().name(), criterio);
    }

    private boolean coincideFiltroPublicacion(
            PublicacionEvento publicacion,
            String q,
            String tipoEvento,
            Integer mes,
            Integer anio) {
        SolicitudEvento solicitud = publicacion.getSolicitudEvento();
        if (tipoEvento != null && !tipoEvento.isBlank()
                && (solicitud.getTipoEventoCatalogo() == null || !solicitud.getTipoEventoCatalogo().getNombre().equalsIgnoreCase(tipoEvento.trim()))) {
            return false;
        }
        if (mes != null && solicitud.getFechaEvento().getMonthValue() != mes) {
            return false;
        }
        if (anio != null && solicitud.getFechaEvento().getYear() != anio) {
            return false;
        }
        if (q == null || q.isBlank()) {
            return true;
        }

        String criterio = q.toLowerCase(Locale.ROOT).trim();
        return contiene(publicacion.getTituloVisible(), criterio)
                || contiene(publicacion.getDescripcionVisible(), criterio)
                || contiene(solicitud.getNombreEvento(), criterio)
                || solicitud.getLugaresFisicos().stream().anyMatch(l -> contiene(l.getNombre(), criterio))
                || contiene(solicitud.getOficina().getNombre(), criterio)
                || contiene(solicitud.getTipoEventoCatalogo() != null ? solicitud.getTipoEventoCatalogo().getNombre() : null, criterio);
    }

    private boolean contiene(String valor, String criterio) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(criterio);
    }

    // --- ENRIQUECIMIENTO DE RESPUESTAS CON VISIBILIDAD ---

    private SolicitudEventoResponse enrichResponse(SolicitudEventoResponse response) {
        if (response == null) return null;
        publicacionEventoRepository.findBySolicitudEventoId(response.getId())
                .ifPresent(p -> response.setVisible(p.getVisible()));
        return response;
    }

    private List<SolicitudEventoResponse> enrichResponseList(List<SolicitudEventoResponse> responses) {
        if (responses == null || responses.isEmpty()) return responses;
        
        List<Long> ids = responses.stream().map(SolicitudEventoResponse::getId).toList();
        List<com.calendario.callapp.callapp_backend.entity.PublicacionEvento> publicaciones = 
                publicacionEventoRepository.findBySolicitudEventoIdIn(ids);
        
        java.util.Map<Long, Boolean> visibilityMap = publicaciones.stream()
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getSolicitudEvento().getId(),
                        com.calendario.callapp.callapp_backend.entity.PublicacionEvento::getVisible,
                        (v1, v2) -> v1 // En caso de duplicados, mantener el primero
                ));
        
        responses.forEach(r -> r.setVisible(visibilityMap.getOrDefault(r.getId(), false)));
        return responses;
    }

    private void triggerImportantEventPush(PublicacionEvento publicacion) {
        if (publicacion == null) return;
        SolicitudEvento solicitud = publicacion.getSolicitudEvento();
        if (solicitud != null && Boolean.TRUE.equals(solicitud.getEsImportante())) {
            try {
                List<String> tokens = dispositivoUsuarioRepository.findAllTokens();

                if (!tokens.isEmpty()) {
                    String title = "🚨 ¡Evento Importante! " + publicacion.getTituloVisible();
                    String body = "Se ha publicado un nuevo evento importante en CallApp: " + solicitud.getNombreEvento();
                    
                    java.util.Map<String, String> data = java.util.Map.of(
                            "type", "IMPORTANT_EVENT",
                            "eventId", String.valueOf(publicacion.getId()),
                            "title", publicacion.getTituloVisible(),
                            "click_action", "FLUTTER_NOTIFICATION_CLICK"
                    );

                    pushNotificationService.sendMulticastNotification(tokens, title, body, data);
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(SolicitudEventoServiceImpl.class)
                        .error("Error al enviar notificación push multicast para evento importante {}: {}", 
                               solicitud.getId(), e.getMessage());
            }
        }
    }
}

