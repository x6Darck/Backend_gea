package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de {@link SolicitudEvento} — solicitudes de evento y sus agregaciones para reportes.
 *
 * <p>Los métodos con {@code JOIN FETCH} precargan asociaciones críticas (oficina, tipo de evento,
 * usuario solicitante) para evitar el problema N+1. Los métodos de reportes devuelven
 * {@code List<Object[]>} que {@code ReporteServiceImpl} mapea a DTOs.</p>
 */
public interface SolicitudEventoRepository extends JpaRepository<SolicitudEvento, Long> {

    /** Carga una solicitud por ID con sus asociaciones principales precargadas. */
    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioSolicitante u WHERE s.id = :id")
    java.util.Optional<SolicitudEvento> getByIdOptimized(@Param("id") Long id);

    /** Devuelve todas las solicitudes de una oficina con sus asociaciones precargadas. */
    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioSolicitante u WHERE s.oficina.id = :oficinaId ORDER BY s.fechaCreacion DESC")
    List<SolicitudEvento> getAllByOficinaIdOptimized(@Param("oficinaId") Long oficinaId);

    /** Devuelve todas las solicitudes paginadas con asociaciones precargadas. */
    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioSolicitante u ORDER BY s.fechaCreacion DESC")
    List<SolicitudEvento> getAllUniqueWithAssociations(Pageable pageable);

    long countByEstado(EstadoSolicitud estado);
    long countByOficinaIdAndEstado(Long oficinaId, EstadoSolicitud estado);
    long countByOficinaId(Long oficinaId);

    /** Conteo global por estado. Formato: {@code [EstadoSolicitud, Long]}. */
    @Query("SELECT s.estado, COUNT(s) FROM SolicitudEvento s GROUP BY s.estado")
    List<Object[]> countByEstadoGrouped();

    /** Conteo por estado para una oficina. Formato: {@code [EstadoSolicitud, Long]}. */
    @Query("SELECT s.estado, COUNT(s) FROM SolicitudEvento s WHERE s.oficina.id = :oficinaId GROUP BY s.estado")
    List<Object[]> countByEstadoGroupedByOficina(@Param("oficinaId") Long oficinaId);

    // --- Métodos para generación de reportes ---
    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.oficina.id = :oficinaId AND s.fechaCreacion BETWEEN :desde AND :hasta AND s.tipoEventoCatalogo.id = :tipoEventoId")
    List<SolicitudEvento> findByOficinaIdAndFechaCreacionBetweenAndTipoEventoCatalogoId(
            @Param("oficinaId") Long oficinaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("tipoEventoId") Long tipoEventoId);

    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.oficina.id = :oficinaId AND s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudEvento> findByOficinaIdAndFechaCreacionBetween(
            @Param("oficinaId") Long oficinaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** Solicitudes globales en rango de fechas filtradas por tipo de evento. */
    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.fechaCreacion BETWEEN :desde AND :hasta AND s.tipoEventoCatalogo.id = :tipoEventoId")
    List<SolicitudEvento> findByFechaCreacionBetweenAndTipoEventoCatalogoId(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("tipoEventoId") Long tipoEventoId);

    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudEvento> findByFechaCreacionBetween(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudEvento> findByFechaCreacionBetweenOrderByFechaCreacionDesc(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    List<SolicitudEvento> findByOficinaIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
            @Param("oficinaId") Long oficinaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
    /** Conteo por tipo de evento filtrado. Formato: {@code [nombreTipo, Long, colorHex]}. */
    @Query("SELECT COALESCE(s.tipoEventoCatalogo.nombre, 'Sin Tipo'), COUNT(s), COALESCE(s.tipoEventoCatalogo.colorHex, '#64748b') FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.tipoEventoCatalogo.nombre, s.tipoEventoCatalogo.colorHex")
    List<Object[]> countEventosByTipoFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Conteo por oficina filtrado. Formato: {@code [nombreOficina, Long]}. */
    @Query("SELECT COALESCE(s.oficina.nombre, 'General'), COUNT(s) FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.oficina.nombre")
    List<Object[]> countEventosByOficinaFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Conteo por estado filtrado. Formato: {@code [EstadoSolicitud, Long]}. */
    @Query("SELECT s.estado, COUNT(s) FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.estado")
    List<Object[]> countEventosByEstadoFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Conteo por mes (1–12) filtrado. Formato: {@code [Integer mes, Long]}. */
    @Query("SELECT MONTH(s.fechaCreacion), COUNT(s) FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY MONTH(s.fechaCreacion)")
    List<Object[]> countEventosByMesFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Total de eventos filtrado por oficina opcional y rango de fechas. */
    @Query("SELECT COUNT(s) FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta")
    long countFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Total de eventos en ciertos estados dentro del rango filtrado. */
    @Query("SELECT COUNT(s) FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.estado IN (:estados) AND s.fechaCreacion BETWEEN :desde AND :hasta")
    long countByEstadosFiltered(@Param("oficinaId") Long oficinaId, @Param("estados") List<EstadoSolicitud> estados, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /**
     * Detecta conflictos de lugar y horario en bloque para un conjunto de fechas.
     * Comprueba solapamiento de horario ({@code horaInicio < horaFin_candidata AND horaFin > horaInicio_candidata})
     * en los estados APROBADA, PUBLICADA o EN_REVISION. Si {@code id} no es nulo, excluye esa solicitud
     * (útil al editar para no conflictuar consigo misma).
     *
     * @param fechas     fechas a verificar
     * @param lugarIds   IDs de lugares físicos a verificar
     * @param horaInicio hora de inicio del evento candidato
     * @param horaFin    hora de fin del evento candidato
     * @param id         ID a excluir de la búsqueda (nulo si es creación)
     * @return lista de solicitudes que colisionan
     */
    @Query("SELECT s FROM SolicitudEvento s JOIN s.lugaresFisicos l WHERE s.fechaEvento IN :fechas " +
           "AND l.id IN :lugarIds " +
           "AND s.estado IN (com.calendario.callapp.callapp_backend.entity.EstadoSolicitud.APROBADA, com.calendario.callapp.callapp_backend.entity.EstadoSolicitud.PUBLICADA, com.calendario.callapp.callapp_backend.entity.EstadoSolicitud.EN_REVISION) " +
           "AND ((s.horaInicio < :horaFin AND s.horaFin > :horaInicio)) " +
           "AND (:id IS NULL OR s.id <> :id)")
    List<SolicitudEvento> findConflictsBulk(@Param("fechas") List<java.time.LocalDate> fechas,
                                            @Param("lugarIds") List<Long> lugarIds,
                                            @Param("horaInicio") java.time.LocalTime horaInicio,
                                            @Param("horaFin") java.time.LocalTime horaFin,
                                            @Param("id") Long id);

    /** Devuelve todos los miembros de una serie recurrente por su ID de grupo. */
    List<SolicitudEvento> findAllByIdGrupoRecurrencia(String idGrupoRecurrencia);

    /** Devuelve todas las solicitudes que usan un tipo de evento; usado para validar borrado de tipo. */
    List<SolicitudEvento> findByTipoEventoCatalogoId(Long tipoEventoCatalogoId);

    /** Devuelve los miembros de una serie con sus participantes precargados, ordenados por fecha. */
    @Query("SELECT DISTINCT s FROM SolicitudEvento s LEFT JOIN FETCH s.participantes WHERE s.idGrupoRecurrencia = :idGrupo ORDER BY s.fechaEvento ASC")
    List<SolicitudEvento> findAllByIdGrupoRecurrenciaConParticipantes(@Param("idGrupo") String idGrupo);

    /** Devuelve los miembros de una serie con sus lugares físicos precargados, ordenados por fecha. */
    @Query("SELECT DISTINCT s FROM SolicitudEvento s LEFT JOIN FETCH s.lugaresFisicos WHERE s.idGrupoRecurrencia = :idGrupo ORDER BY s.fechaEvento ASC")
    List<SolicitudEvento> findAllByIdGrupoRecurrenciaConLugares(@Param("idGrupo") String idGrupo);
}
