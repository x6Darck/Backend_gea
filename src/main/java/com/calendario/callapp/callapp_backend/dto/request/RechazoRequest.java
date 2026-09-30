package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/** Motivo obligatorio para rechazar una solicitud de evento o anuncio. */
public class RechazoRequest {

    @NotBlank
    private String motivo;
}
