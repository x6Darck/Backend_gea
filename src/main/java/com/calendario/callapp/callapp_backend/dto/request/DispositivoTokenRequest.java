package com.calendario.callapp.callapp_backend.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
/** Token FCM del dispositivo móvil para registro o desregistro de notificaciones push. */
public class DispositivoTokenRequest {

    @NotBlank(message = "El token del dispositivo es obligatorio")
    private String token;
}
