package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.LugarFisico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de {@link LugarFisico} — espacios físicos (salones, auditorios, campus externos).
 *
 * <p>Usado por {@code LugarFisicoServiceImpl} para el CRUD del catálogo y por
 * {@code SolicitudEventoServiceImpl} para validar disponibilidad de lugares.</p>
 */
public interface LugarFisicoRepository extends JpaRepository<LugarFisico, Long> {

    /** Devuelve únicamente los lugares activos, ordenados alfabéticamente. */
    List<LugarFisico> findByActivoTrueOrderByNombreAsc();

    /** Busca un lugar por nombre sin distinguir mayúsculas; usado para detectar duplicados. */
    Optional<LugarFisico> findByNombreIgnoreCase(String nombre);
}
