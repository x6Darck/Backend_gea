package com.calendario.callapp.callapp_backend.repository;


import com.calendario.callapp.callapp_backend.entity.ReporteGenerado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repositorio de {@link ReporteGenerado} — historial de reportes generados por los usuarios.
 *
 * <p>Todos los métodos usan {@code LEFT JOIN FETCH} sobre {@code usuarioGenerador}
 * y su {@code oficina} para evitar consultas N+1 al serializar la lista de reportes.</p>
 */
public interface ReporteGeneradoRepository extends JpaRepository<ReporteGenerado, Long> {

    /** Sobreescribe {@code findAll()} para incluir el usuario generador y su oficina en una sola consulta. */
    @Override
    @org.springframework.lang.NonNull
    @Query("SELECT r FROM ReporteGenerado r LEFT JOIN FETCH r.usuarioGenerador u LEFT JOIN FETCH u.oficina")
    List<ReporteGenerado> findAll();

    /** Sobreescribe {@code findById()} para incluir el usuario generador y su oficina. */
    @Override
    @org.springframework.lang.NonNull
    @Query("SELECT r FROM ReporteGenerado r LEFT JOIN FETCH r.usuarioGenerador u LEFT JOIN FETCH u.oficina WHERE r.id = :id")
    java.util.Optional<ReporteGenerado> findById(@org.springframework.lang.NonNull @org.springframework.data.repository.query.Param("id") Long id);

    /** Devuelve los reportes de un usuario específico, del más reciente al más antiguo. */
    @Query("SELECT r FROM ReporteGenerado r LEFT JOIN FETCH r.usuarioGenerador u LEFT JOIN FETCH u.oficina WHERE u.id = :usuarioId ORDER BY r.fechaCreacion DESC")
    List<ReporteGenerado> findByUsuarioGeneradorIdOrderByFechaCreacionDesc(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);

    /** Devuelve los reportes generados por usuarios de una oficina específica, del más reciente al más antiguo. */
    @Query("SELECT r FROM ReporteGenerado r LEFT JOIN FETCH r.usuarioGenerador u LEFT JOIN FETCH u.oficina WHERE u.oficina.id = :oficinaId ORDER BY r.fechaCreacion DESC")
    List<ReporteGenerado> findByUsuarioGeneradorOficinaIdOrderByFechaCreacionDesc(@org.springframework.data.repository.query.Param("oficinaId") Long oficinaId);

    /** Devuelve los reportes creados en el rango de fechas dado, del más reciente al más antiguo. */
    @Query("SELECT r FROM ReporteGenerado r LEFT JOIN FETCH r.usuarioGenerador u LEFT JOIN FETCH u.oficina WHERE r.fechaCreacion BETWEEN :desde AND :hasta ORDER BY r.fechaCreacion DESC")
    List<ReporteGenerado> findByFechaCreacionBetween(
            @org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde,
            @org.springframework.data.repository.query.Param("hasta") java.time.LocalDateTime hasta);
}
