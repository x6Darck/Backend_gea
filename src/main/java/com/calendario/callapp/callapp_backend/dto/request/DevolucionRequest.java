package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/** Observaciones obligatorias para devolver una solicitud a estado {@code PENDIENTE} (devolución). */
public class DevolucionRequest {

    @NotBlank
    private String observaciones;
}
