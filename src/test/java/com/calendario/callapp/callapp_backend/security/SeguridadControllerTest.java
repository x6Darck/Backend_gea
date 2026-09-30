package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private HistorialLoginRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();

        // 'otro' se guarda primero (fecha más antigua) para que 'visible' quede
        // de primero al ordenar por fecha DESC (ver HistorialLoginRepository.buscar).
        HistorialLogin otro = new HistorialLogin();
        otro.setCorreoIntentado("otro.usuario@gea.edu.co");
        otro.setExito(false);
        otro.setMetodo(AuthProvider.LOCAL);
        otro.setIp("10.0.0.9");
        otro.setFecha(LocalDateTime.now().minusMinutes(1));
        repository.save(otro);

        HistorialLogin visible = new HistorialLogin();
        visible.setCorreoIntentado("visible.panel@gea.edu.co");
        visible.setExito(true);
        visible.setMetodo(AuthProvider.LOCAL);
        visible.setIp("200.1.2.3");
        visible.setFecha(LocalDateTime.now());
        repository.save(visible);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void superadmin_puede_ver_el_historial() throws Exception {
        mockMvc.perform(get("/admin/seguridad/historial-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].correoIntentado").value("visible.panel@gea.edu.co"));
    }

    @Test
    @WithMockUser(roles = "COMUNICACIONES")
    void otro_rol_recibe_403() throws Exception {
        mockMvc.perform(get("/admin/seguridad/historial-login"))
                .andExpect(status().isForbidden());
    }

    /**
     * COMUNICACIONES ya cae fuera en el gate genérico de {@code /admin/**}
     * (ver {@code SecurityConfig}), así que ese 403 no prueba nada específico
     * de este endpoint. ADMIN sí atraviesa ese gate genérico (que permite
     * SUPER_ADMIN y ADMIN) pero debe seguir siendo rechazado por el
     * {@code @PreAuthorize("hasRole('SUPER_ADMIN')")} propio de este endpoint,
     * que es más restrictivo que /admin/auditoria (el cual sí admite ADMIN).
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_recibe_403_por_ser_mas_restrictivo_que_el_gate_generico() throws Exception {
        mockMvc.perform(get("/admin/seguridad/historial-login"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void filtro_por_correo_excluye_los_no_coincidentes() throws Exception {
        mockMvc.perform(get("/admin/seguridad/historial-login").param("correo", "visible.panel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].correoIntentado").value("visible.panel@gea.edu.co"));
    }
}
