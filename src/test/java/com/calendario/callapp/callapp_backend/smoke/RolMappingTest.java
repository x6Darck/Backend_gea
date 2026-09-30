package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.entity.Rol;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RolMappingTest {

    @Test
    void fromNombre_mapea_consultoria() {
        assertThat(Rol.fromNombre("Consultoria")).isEqualTo(Rol.CONSULTORIA);
    }

    @Test
    void consultoria_security_role_es_CONSULTORIA() {
        assertThat(Rol.CONSULTORIA.getSecurityRole()).isEqualTo("CONSULTORIA");
    }

    @Test
    void consultoria_no_es_administrador_ni_oficina() {
        assertThat(Rol.CONSULTORIA.esAdministradorGlobal()).isFalse();
        assertThat(Rol.CONSULTORIA.esOficina()).isFalse();
        assertThat(Rol.CONSULTORIA.esComunicaciones()).isFalse();
    }
}
