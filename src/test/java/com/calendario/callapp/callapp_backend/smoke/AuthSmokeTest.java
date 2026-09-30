package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthSmokeTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void contexto_carga_correctamente() {
        // Si el contexto Spring no arranca (JWT mal config, DB no disponible, etc.)
        // este test falla con error de contexto antes de llegar aquí
        assertThat(jwtService).isNotNull();
    }

    @Test
    void jwt_token_generado_y_validado_correctamente() {
        // Rol es un @Transient derivado de RolEntity — no existe setRol().
        // Se construye un RolEntity con nombre "USUARIO_APP" para que getRol()
        // retorne Rol.USUARIO_APP via Rol.fromNombre().
        RolEntity rolEntity = new RolEntity();
        rolEntity.setNombre("USUARIO_APP");

        Usuario usuario = new Usuario();
        usuario.setCorreo("test@gea.edu.co");
        usuario.setRolEntity(rolEntity);

        String token = jwtService.generarToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@gea.edu.co");
        assertThat(jwtService.extractRol(token)).isNotBlank();
    }
}
