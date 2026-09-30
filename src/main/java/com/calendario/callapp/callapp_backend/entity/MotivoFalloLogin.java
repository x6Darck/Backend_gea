package com.calendario.callapp.callapp_backend.entity;

/**
 * Motivo por el que un intento de login fue rechazado. Se guarda solo en el
 * historial admin-only ({@link HistorialLogin}); al usuario final el login
 * sigue devolviendo un mensaje genérico (defensa anti-enumeración).
 *
 * <ul>
 *   <li>{@code CORREO_NO_REGISTRADO} — el correo no corresponde a ningún usuario registrado.</li>
 *   <li>{@code CUENTA_INACTIVA} — el usuario existe pero su cuenta está inactiva.</li>
 *   <li>{@code CREDENCIALES_INVALIDAS} — contraseña incorrecta (login local).</li>
 *   <li>{@code TOKEN_INVALIDO} — token de Microsoft inválido, expirado o de otra audiencia.</li>
 *   <li>{@code OTRO} — cualquier otro fallo no clasificado.</li>
 * </ul>
 */
public enum MotivoFalloLogin {
    CORREO_NO_REGISTRADO,
    CUENTA_INACTIVA,
    CREDENCIALES_INVALIDAS,
    TOKEN_INVALIDO,
    OTRO
}
