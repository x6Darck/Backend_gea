package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
/**
 * Respuesta de autenticación exitosa.
 *
 * <p>Contiene el JWT ({@code token}), datos del usuario (nombre, correo, rol, oficina, foto)
 * y el tipo de token ({@code "Bearer"}). El token también se envía como cookie {@code gea_auth}
 * para clientes web; este cuerpo lo usa la app móvil Flutter.</p>
 */
public class AuthResponse {

    private Long id;

    private String token;

    private String nombre;

    private String correo;

    private String rol;
    
    private Long idOficina;
    
    private String oficinaNombre;
    
    private String fotoUrl;

    @Builder.Default
    private String tipo = "Bearer";
}