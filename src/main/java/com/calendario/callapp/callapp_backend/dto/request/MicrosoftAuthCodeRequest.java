package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * Código de autorización obtenido por la app móvil tras el login interactivo
 * con Microsoft (WebView), antes de que se intercambie por un token — ese
 * intercambio lo hace el backend con {@code client_secret}, ver
 * {@link com.calendario.callapp.callapp_backend.service.impl.MicrosoftTokenExchangeService}.
 */
public class MicrosoftAuthCodeRequest {

    @NotBlank
    private String code;
}
