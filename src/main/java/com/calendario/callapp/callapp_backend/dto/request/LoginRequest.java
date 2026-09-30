package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/** DTO alternativo de autenticación (alias de {@link AuthRequest}). Usado por algunos endpoints legacy. */
public class LoginRequest {

    @NotBlank
    private String correo;

    @NotBlank
    private String password;
}