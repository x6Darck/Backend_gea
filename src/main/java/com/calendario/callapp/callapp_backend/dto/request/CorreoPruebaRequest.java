package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/** Datos para enviar un correo de prueba del sistema de notificaciones. Solo disponible en entornos de desarrollo. */
public class CorreoPruebaRequest {

    @Email
    @NotBlank
    private String destinatario;

    private String titulo;

    private String lugar;

    private String oficina;
}
