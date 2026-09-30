package com.calendario.callapp.callapp_backend.entity;

/**
 * Proveedor de identidad utilizado para autenticar al usuario.
 *
 * <ul>
 *   <li>{@code LOCAL} — credenciales propias del sistema (correo + contraseña bcrypt).</li>
 *   <li>{@code MICROSOFT} — OAuth2 con cuenta Microsoft; el identificador externo
 *       se guarda en {@link Usuario#getMicrosoftOid()}.</li>
 * </ul>
 */
public enum AuthProvider {
    LOCAL,
    MICROSOFT
}
