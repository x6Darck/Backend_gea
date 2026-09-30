package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/** Contiene el {@code idToken} JWT emitido por Microsoft tras el flujo PKCE OAuth2 en la app móvil. */
public class MicrosoftAuthRequest {

    @NotBlank
    private String idToken;
}
