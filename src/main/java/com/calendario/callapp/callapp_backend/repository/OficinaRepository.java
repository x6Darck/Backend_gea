package com.calendario.callapp.callapp_backend.repository;

import java.util.List;
import java.util.Optional;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de {@link Oficina} — dependencias y programas académicos.
 *
 * <p>Usado por {@code OficinaServiceImpl} para el CRUD y por
 * {@code SolicitudEventoServiceImpl} para obtener la oficina del solicitante.</p>
 */
public interface OficinaRepository extends JpaRepository<Oficina, Long> {

    /** Busca una oficina por nombre sin distinguir mayúsculas; usado para detectar duplicados. */
    Optional<Oficina> findByNombreIgnoreCase(String nombre);

    /** Devuelve únicamente las oficinas activas, ordenadas alfabéticamente. */
    List<Oficina> findByActivaTrueOrderByNombreAsc();
}
