package com.calendario.callapp.callapp_backend.entity;

/**
 * Enum de roles de negocio del sistema GEA.
 *
 * <p>No se persiste directamente: {@link RolEntity} guarda el nombre en base de
 * datos y {@link #fromNombre(String)} lo traduce a este enum. Las anotaciones
 * {@code @PreAuthorize} de los controllers usan los strings devueltos por
 * {@link #getSecurityRole()} con el prefijo {@code ROLE_} que añade Spring Security.</p>
 *
 * <ul>
 *   <li>{@code SUPER_ADMIN} / {@code ADMIN} — administradores con acceso total.</li>
 *   <li>{@code COMUNICACIONES} — equipo que revisa, aprueba y publica solicitudes.</li>
 *   <li>{@code OFICINA} — dependencias que crean solicitudes de evento.</li>
 *   <li>{@code USUARIO_AUTENTICADO_APP} — usuario de la app móvil con acceso a solicitudes.</li>
 *   <li>{@code CONSULTORIA} — acceso de solo lectura global.</li>
 *   <li>{@code USUARIO_APP} — usuario básico de la app, puede hacer solicitudes de anuncio.</li>
 *   <li>{@code USUARIO} — rol residual para usuarios sin clasificación explícita.</li>
 * </ul>
 */
public enum Rol {
    SUPER_ADMIN,
    COMUNICACIONES,
    OFICINA,
    USUARIO_APP,
    USUARIO_AUTENTICADO_APP,
    ADMIN,
    CONSULTORIA,
    USUARIO;

    public static Rol fromNombre(String nombre) {
        if (nombre == null) return Rol.USUARIO;
        
        return switch (nombre) {
            case "SuperAdmin" -> SUPER_ADMIN;
            case "Comunicaciones" -> COMUNICACIONES;
            case "Oficina" -> OFICINA;
            case "Usuario Autenticado" -> USUARIO_AUTENTICADO_APP;
            case "Admin" -> ADMIN;
            case "Consultoria" -> CONSULTORIA;
            case "Usuario" -> USUARIO;
            default -> {
                try {
                    yield Rol.valueOf(nombre.toUpperCase());
                } catch (Exception e) {
                    yield Rol.USUARIO;
                }
            }
        };
    }

    public String getSecurityRole() {
        return switch (this) {
            case ADMIN -> "SUPER_ADMIN";
            case USUARIO -> "USUARIO_APP";
            default -> this.name();
        };
    }

    public boolean esAdministradorGlobal() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean esComunicaciones() {
        return this == COMUNICACIONES;
    }

    public boolean esOficina() {
        return this == OFICINA || this == USUARIO_AUTENTICADO_APP || this == COMUNICACIONES;
    }

    /** Acceso de lectura global: admins, comunicaciones y consultoría ven todos los registros. */
    public boolean tieneAccesoGlobal() {
        return this == SUPER_ADMIN || this == ADMIN || this == COMUNICACIONES || this == CONSULTORIA;
    }
}
