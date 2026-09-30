package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.TipoEventoCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de {@link TipoEventoCatalogo} — catálogo de tipos de evento (Congreso, Taller, etc.).
 *
 * <p>Usado por {@code TipoEventoServiceImpl} para CRUD y por
 * {@code SolicitudEventoServiceImpl} para resolver el tipo al crear una solicitud.</p>
 */
public interface TipoEventoCatalogoRepository extends JpaRepository<TipoEventoCatalogo, Long> {

    /** Devuelve los tipos activos en orden alfabético; usado en los selectores del frontend y la app. */
    List<TipoEventoCatalogo> findByActivoTrueOrderByNombreAsc();

    /** Busca un tipo activo por nombre (case-insensitive); usado al crear solicitudes con nombre de tipo. */
    Optional<TipoEventoCatalogo> findByNombreIgnoreCaseAndActivoTrue(String nombre);

    /** Busca un tipo (activo o inactivo) por nombre; usado para detectar duplicados al crear o editar. */
    Optional<TipoEventoCatalogo> findByNombreIgnoreCase(String nombre);
}
