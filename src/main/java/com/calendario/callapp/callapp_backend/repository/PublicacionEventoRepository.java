package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.PublicacionEvento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de {@link PublicacionEvento} — eventos publicados visibles al público.
 *
 * <p>Todas las queries con {@code JOIN FETCH} precargan en la misma consulta SQL las
 * asociaciones to-one {@code solicitudEvento}, {@code oficina}, {@code tipoEventoCatalogo}
 * y {@code usuarioSolicitante} — evita el problema N+1 al serializar la lista de eventos
 * al cliente, confirmado con pruebas de carga reales. {@code lugaresFisicos} (to-many)
 * NO entra en estos JOIN FETCH a propósito — mezclarla rompería el LIMIT/OFFSET de los
 * métodos paginados (Hibernate deja de poder aplicar el límite en SQL cuando hay un JOIN
 * a una colección, y trae todas las filas a memoria: warning HHH90003004). En su lugar se
 * carga sola, en un único query por lote, vía {@code @BatchSize} en
 * {@link com.calendario.callapp.callapp_backend.entity.SolicitudEvento#getLugaresFisicos()}.</p>
 */
public interface PublicacionEventoRepository extends JpaRepository<PublicacionEvento, Long> {

    /** Devuelve la publicación de una solicitud de evento específica. */
    Optional<PublicacionEvento> findBySolicitudEventoId(Long solicitudEventoId);

    /** Carga las publicaciones de varias solicitudes de evento en una sola consulta (IN). */
    List<PublicacionEvento> findBySolicitudEventoIdIn(List<Long> solicitudEventoIds);

    /**
     * Elimina en bloque las publicaciones de un conjunto de solicitudes.
     * Usado al eliminar una serie recurrente para limpiar las publicaciones asociadas.
     *
     * @param ids lista de IDs de solicitudes cuyas publicaciones se eliminan
     */
    @Modifying
    @Query("DELETE FROM PublicacionEvento p WHERE p.solicitudEvento.id IN :ids")
    void deleteBySolicitudEventoIdIn(@Param("ids") List<Long> ids);

    /**
     * Devuelve eventos visibles en un rango de fechas con paginación.
     * Ordena: importantes primero, luego por fecha y hora de inicio.
     *
     * @param inicio  primer día del rango (inclusive)
     * @param fin     último día del rango (exclusive)
     * @param pageable configuración de página (tamaño/número)
     */
    @Query("""
            SELECT p
            FROM PublicacionEvento p
            JOIN FETCH p.solicitudEvento s
            JOIN FETCH s.oficina
            JOIN FETCH s.tipoEventoCatalogo
            JOIN FETCH s.usuarioSolicitante
            WHERE p.visible = true
              AND s.fechaEvento >= :inicio
              AND s.fechaEvento < :fin
            ORDER BY s.esImportante DESC, s.fechaEvento ASC, s.horaInicio ASC
            """)
    List<PublicacionEvento> findPublicadasEnRango(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin, Pageable pageable);

    /**
     * Devuelve los próximos eventos visibles desde {@code hoy} en adelante, paginado.
     * Usado en {@code GET /app/eventos/proximos}.
     *
     * @param hoy  fecha de corte (se incluyen eventos en esa fecha o posterior)
     * @param pageable límite de resultados
     */
    @Query("""
            SELECT p
            FROM PublicacionEvento p
            JOIN FETCH p.solicitudEvento s
            JOIN FETCH s.oficina
            JOIN FETCH s.tipoEventoCatalogo
            JOIN FETCH s.usuarioSolicitante
            WHERE p.visible = true
              AND s.fechaEvento >= :hoy
            ORDER BY s.esImportante DESC, s.fechaEvento ASC, s.horaInicio ASC
            """)
    List<PublicacionEvento> findProximos(@Param("hoy") LocalDate hoy, Pageable pageable);

    /**
     * Devuelve eventos visibles en un rango de fechas sin paginación.
     * Variante usada por {@code AgendaPdfService} para exportar la agenda completa.
     *
     * @param inicio primer día del rango (inclusive)
     * @param fin    último día del rango (exclusive)
     */
    @Query("""
            SELECT p
            FROM PublicacionEvento p
            JOIN FETCH p.solicitudEvento s
            JOIN FETCH s.oficina
            JOIN FETCH s.tipoEventoCatalogo
            JOIN FETCH s.usuarioSolicitante
            WHERE p.visible = true
              AND s.fechaEvento >= :inicio
              AND s.fechaEvento < :fin
            ORDER BY s.esImportante DESC, s.fechaEvento ASC, s.horaInicio ASC
            """)
    List<PublicacionEvento> findPublicadasEnRango(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    /** Devuelve todos los eventos visibles paginados, importantes primero. */
    @Query("SELECT p FROM PublicacionEvento p JOIN FETCH p.solicitudEvento s JOIN FETCH s.oficina JOIN FETCH s.tipoEventoCatalogo JOIN FETCH s.usuarioSolicitante WHERE p.visible = true ORDER BY s.esImportante DESC, p.fechaPublicacion DESC")
    List<PublicacionEvento> getAllVisibleOptimized(Pageable pageable);

    /** Busca un evento visible por su ID de publicación; retorna vacío si está oculto. */
    @Query("SELECT p FROM PublicacionEvento p JOIN FETCH p.solicitudEvento s JOIN FETCH s.oficina JOIN FETCH s.tipoEventoCatalogo JOIN FETCH s.usuarioSolicitante WHERE p.id = :id AND p.visible = true")
    Optional<PublicacionEvento> getByIdAndVisibleUnique(@Param("id") Long id);
}
