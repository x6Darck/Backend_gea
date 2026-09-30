package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de {@link Usuario} — cuentas de usuario del sistema.
 *
 * <p>Los métodos de búsqueda con {@code JOIN FETCH} precargan {@code rolEntity} y {@code oficina}
 * para evitar N+1 al leer el usuario autenticado en cada petición.</p>
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Carga un usuario por correo con rol y oficina precargados.
     * Usado por {@code CustomUserDetailsService} en cada validación de token JWT.
     *
     * @param correo correo electrónico del usuario
     * @return el usuario si existe, vacío en caso contrario
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rolEntity LEFT JOIN FETCH u.oficina WHERE u.correo = :correo")
    Optional<Usuario> getByCorreoOptimized(@Param("correo") String correo);

    /** Verifica si ya existe un usuario con ese correo (para unicidad al crear/editar). */
    boolean existsByCorreo(String correo);

    /** Verifica si ya existe un usuario con ese teléfono (para unicidad al crear/editar). */
    boolean existsByTelefono(String telefono);

    /** Verifica si ya existe un usuario con ese OID de Microsoft (para unicidad OAuth2). */
    boolean existsByMicrosoftOid(String microsoftOid);

    /**
     * Busca usuario por teléfono excluyendo un ID específico.
     * Usado al editar para detectar conflictos de teléfono sin fallar en el propio usuario.
     *
     * @param telefono   teléfono a buscar
     * @param excludeId  ID del usuario a excluir (nulo para no excluir ninguno)
     */
    @Query("SELECT u FROM Usuario u WHERE u.telefono = :telefono AND (:excludeId IS NULL OR u.id <> :excludeId)")
    java.util.Optional<Usuario> findByTelefonoExcluyendo(@Param("telefono") String telefono, @Param("excludeId") Long excludeId);

    /**
     * Busca usuario por Microsoft OID excluyendo un ID específico.
     * Usado al editar para detectar conflictos de OID sin fallar en el propio usuario.
     *
     * @param oid        OID de Microsoft a buscar
     * @param excludeId  ID del usuario a excluir (nulo para no excluir ninguno)
     */
    @Query("SELECT u FROM Usuario u WHERE u.microsoftOid = :oid AND (:excludeId IS NULL OR u.id <> :excludeId)")
    java.util.Optional<Usuario> findByMicrosoftOidExcluyendo(@Param("oid") String oid, @Param("excludeId") Long excludeId);

    /** Devuelve el número de usuarios pertenecientes a una oficina; usado para validar borrado de oficina. */
    long countByOficinaId(Long oficinaId);

    /**
     * Búsqueda filtrada de usuarios con rol, estado y texto libre (nombre o correo).
     * Todos los parámetros son opcionales; si son nulos, no se aplica ese filtro.
     *
     * @param q        texto libre sobre nombre o correo (case-insensitive, búsqueda parcial)
     * @param rolName  nombre de rol exacto (p. ej. {@code "Comunicaciones"})
     * @param estado   estado de la cuenta (p. ej. {@code "activo"})
     * @return lista de usuarios que cumplen todos los filtros aplicados
     */
    @Query("SELECT u FROM Usuario u " +
           "JOIN FETCH u.rolEntity " +
           "LEFT JOIN FETCH u.oficina " +
           "WHERE (:rolName IS NULL OR u.rolEntity.nombre = :rolName) AND " +
           "(:estado IS NULL OR u.estado = :estado) AND " +
           "(:q IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Usuario> searchUsuarios(@Param("q") String q, @Param("rolName") String rolName, @Param("estado") String estado);
}
