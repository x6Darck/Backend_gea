package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/** Datos para crear un tipo de evento en el catálogo. El {@code colorHex} debe seguir el patrón {@code #RRGGBB}. */
public class TipoEventoRequest {

    @NotBlank
    private String nombre;

    private String descripcion;

    @NotBlank(message = "El color es obligatorio")
    @jakarta.validation.constraints.Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser un hexadecimal válido (ej: #CE1126)")
    private String colorHex;

    private Boolean activo;
}
