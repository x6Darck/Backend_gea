package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.PublicacionAnuncio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de {@link PublicacionAnuncio} — anuncios publicados visibles al público.
 *
 * <p>Usado por {@code SolicitudAnuncioServiceImpl} al publicar, ocultar y eliminar
 * publicaciones; y para listar los anuncios visibles en el endpoint público
 * {@code GET /app/anuncios/publicados}.</p>
 *
 * <p>{@code findByVisibleTrueOrderByFechaPublicacionDesc} usa {@code JOIN FETCH} para
 * precargar en la misma consulta SQL {@code solicitudAnuncio}, {@code oficina} y
 * {@code usuarioSolicitante} (con su propia {@code oficina}, usada como fallback) —
 * {@link com.calendario.callapp.callapp_backend.mapper.PublicacionAnuncioMapper} accede a
 * las tres por cada fila del listado; sin el fetch join, eso dispara una consulta SQL
 * adicional por cada anuncio visible (N+1), confirmado con pruebas de carga reales.
 * {@code lugaresFisicos} NO entra en este JOIN FETCH a propósito — es una colección
 * (to-many) y meterla aquí rompería el LIMIT de cualquier paginación futura sobre este
 * método; en cambio se carga sola, en un único query por lote, vía
 * {@code @BatchSize} en {@link com.calendario.callapp.callapp_backend.entity.SolicitudAnuncio#getLugaresFisicos()}.</p>
 */
public interface PublicacionAnuncioRepository extends JpaRepository<PublicacionAnuncio, Long> {

    /** Devuelve la publicación de una solicitud de anuncio específica. */
    Optional<PublicacionAnuncio> findBySolicitudAnuncioId(Long solicitudAnuncioId);

    /** Carga las publicaciones de varias solicitudes de anuncio en una sola consulta (IN). */
    List<PublicacionAnuncio> findBySolicitudAnuncioIdIn(List<Long> solicitudAnuncioIds);

    /** Devuelve todos los anuncios visibles ordenados del más reciente al más antiguo. */
    @Query("""
            SELECT p
            FROM PublicacionAnuncio p
            JOIN FETCH p.solicitudAnuncio s
            LEFT JOIN FETCH s.oficina
            JOIN FETCH s.usuarioSolicitante su
            LEFT JOIN FETCH su.oficina
            WHERE p.visible = true
            ORDER BY p.fechaPublicacion DESC
            """)
    List<PublicacionAnuncio> findByVisibleTrueOrderByFechaPublicacionDesc();

    /** Devuelve un anuncio visible por ID; retorna vacío si está oculto. */
    Optional<PublicacionAnuncio> findByIdAndVisibleTrue(Long id);
}
