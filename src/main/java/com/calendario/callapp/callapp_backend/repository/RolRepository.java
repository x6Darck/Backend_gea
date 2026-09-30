package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de {@link RolEntity} — catálogo de roles del sistema.
 *
 * <p>Usado por {@code DataInitializer} y {@code UsuarioServiceImpl} para
 * asignar roles a usuarios por nombre (p. ej. {@code "Comunicaciones"}).</p>
 */
@Repository
public interface RolRepository extends JpaRepository<RolEntity, Long> {

    /**
     * Busca un rol por su nombre de negocio exacto.
     * El nombre se traduce al enum {@link com.calendario.callapp.callapp_backend.entity.Rol}
     * mediante {@code Rol.fromNombre()}.
     *
     * @param nombre nombre del rol (p. ej. {@code "Comunicaciones"}, {@code "Oficina"})
     * @return el rol si existe, vacío en caso contrario
     */
    Optional<RolEntity> findByNombre(String nombre);
}
