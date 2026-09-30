package com.calendario.callapp.callapp_backend.dto.response;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.Rol;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
/** Proyección de usuario para el cliente. Expone rol, oficina y proveedor de autenticación sin revelar la contraseña. */
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String correo;
    private String telefono;
    private String estado;
    private Rol rol;
    private Long idOficina;
    private String oficinaNombre;
    private AuthProvider authProvider;
    private String fotoUrl;
    private java.time.LocalDateTime fechaCreacion;
    private java.time.LocalDateTime fechaActualizacion;
    private String usuarioCreacion;
    private String usuarioActualizacion;
}
