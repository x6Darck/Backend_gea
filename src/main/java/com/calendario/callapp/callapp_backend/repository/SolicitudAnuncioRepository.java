package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.SolicitudAnuncio;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de {@link SolicitudAnuncio} — solicitudes de anuncio y sus agregaciones para reportes.
 *
 * <p>Los métodos de datos usan {@code JOIN FETCH} para precargar relaciones y evitar N+1.
 * Los métodos de reportes devuelven {@code List<Object[]>} con las columnas del SELECT
 * (estado/oficina/categoría + conteo) que {@code ReporteServiceImpl} mapea a DTOs.</p>
 */
public interface SolicitudAnuncioRepository extends JpaRepository<SolicitudAnuncio, Long> {

    /**
     * Carga una solicitud por ID precargando usuario, oficina y lugares.
     * Evita N+1 al serializar la solicitud con sus asociaciones.
     */
    @Query("SELECT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina LEFT JOIN FETCH s.lugaresFisicos WHERE s.id = :id")
    java.util.Optional<SolicitudAnuncio> getByIdOptimized(@Param("id") Long id);

    /**
     * Devuelve todas las solicitudes de una oficina, incluyendo las creadas por usuarios
     * cuya oficina coincide (caso donde {@code s.oficina} es nula y se infiere del usuario).
     */
    @Query("SELECT DISTINCT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.lugaresFisicos " +
           "WHERE s.oficina.id = :oficinaId OR (s.oficina IS NULL AND u.oficina.id = :oficinaId) ORDER BY s.fechaCreacion DESC")
    List<SolicitudAnuncio> getAllByOficinaIdOptimized(@Param("oficinaId") Long oficinaId);

    /** Devuelve todas las solicitudes paginadas con sus asociaciones precargadas. */
    @Query("SELECT DISTINCT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina LEFT JOIN FETCH s.lugaresFisicos ORDER BY s.fechaCreacion DESC")
    List<SolicitudAnuncio> getAllUniqueWithAssociations(Pageable pageable);

    long countByEstado(EstadoSolicitud estado);
    long countByOficinaId(Long oficinaId);
    long countByOficinaIdAndEstado(Long oficinaId, EstadoSolicitud estado);

    /** Devuelve el conteo de solicitudes agrupado por estado (global). Formato: {@code [EstadoSolicitud, Long]}. */
    @Query("SELECT s.estado, COUNT(s) FROM SolicitudAnuncio s GROUP BY s.estado")
    List<Object[]> countByEstadoGrouped();

    /** Devuelve el conteo por estado para una oficina. Formato: {@code [EstadoSolicitud, Long]}. */
    @Query("SELECT s.estado, COUNT(s) FROM SolicitudAnuncio s WHERE s.oficina.id = :oficinaId GROUP BY s.estado")
    List<Object[]> countByEstadoGroupedByOficina(@Param("oficinaId") Long oficinaId);

    // --- Métodos para generación de reportes ---
    /** Solicitudes de una oficina en un rango de fechas; para reportes por oficina. */
    @Query("SELECT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE (s.oficina.id = :oficinaId OR (s.oficina IS NULL AND s.usuarioSolicitante.oficina.id = :oficinaId)) " +
           "AND s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudAnuncio> findByUsuarioSolicitanteOficinaIdAndFechaCreacionBetween(
            @Param("oficinaId") Long oficinaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudAnuncio> findByFechaCreacionBetween(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudAnuncio> findByFechaCreacionBetweenOrderByFechaCreacionDesc(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    List<SolicitudAnuncio> findByOficinaIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
            @Param("oficinaId") Long oficinaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** Total de anuncios filtrado por oficina opcional y rango de fechas. */
    @Query("SELECT COUNT(s) FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta")
    long countFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Conteo por oficina en el rango filtrado. Formato: {@code [nombreOficina, Long]}. */
    @Query("SELECT COALESCE(s.oficina.nombre, 'General'), COUNT(s) FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.oficina.nombre")
    List<Object[]> countAnunciosByOficinaFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Conteo por estado en el rango filtrado. Formato: {@code [EstadoSolicitud, Long]}. */
    @Query("SELECT s.estado, COUNT(s) FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.estado")
    List<Object[]> countAnunciosByEstadoFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Conteo por mes (número 1–12) en el rango filtrado. Formato: {@code [Integer mes, Long]}. */
    @Query("SELECT MONTH(s.fechaCreacion), COUNT(s) FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY MONTH(s.fechaCreacion)")
    List<Object[]> countAnunciosByMesFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Total de anuncios en ciertos estados dentro del rango filtrado. */
    @Query("SELECT COUNT(s) FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.estado IN (:estados) AND s.fechaCreacion BETWEEN :desde AND :hasta")
    long countByEstadosFiltered(@Param("oficinaId") Long oficinaId, @Param("estados") List<EstadoSolicitud> estados, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Conteo por categoría en el rango filtrado. Formato: {@code [nombreCategoria, Long, colorHex]}. */
    @Query("SELECT COALESCE(s.categoria, 'General'), COUNT(s), '#64748b' FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.categoria")
    List<Object[]> countAnunciosByCategoriaFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
