package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class EstadoSolicitudTest {

    @Test
    void existe_el_estado_en_revision() {
        EstadoSolicitud estado = EstadoSolicitud.valueOf("EN_REVISION");
        assertThat(estado).isNotNull();
    }

    @Test
    void el_enum_conserva_los_estados_previos() {
        assertThat(catchThrowable(() -> EstadoSolicitud.valueOf("PENDIENTE"))).isNull();
        assertThat(catchThrowable(() -> EstadoSolicitud.valueOf("APROBADA"))).isNull();
        assertThat(catchThrowable(() -> EstadoSolicitud.valueOf("RECHAZADA"))).isNull();
        assertThat(catchThrowable(() -> EstadoSolicitud.valueOf("PUBLICADA"))).isNull();
    }
}
