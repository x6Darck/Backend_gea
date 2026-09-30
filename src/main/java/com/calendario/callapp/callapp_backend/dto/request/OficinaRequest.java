package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/** Datos para crear o actualizar una oficina (unidad académica o administrativa). */
public class OficinaRequest {

    @NotBlank(message = "El nombre de la oficina es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Size(max = 200, message = "El programa académico no puede superar 200 caracteres")
    private String programaAcademico;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String descripcion;

    private Boolean activa;
}
