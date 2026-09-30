package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.dto.request.AuthRequest;
import com.calendario.callapp.callapp_backend.dto.response.AuthResponse;
import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.security.JwtService;
import com.calendario.callapp.callapp_backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthFlowTest {

    @Autowired private AuthServiceImpl authService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private static final String TEST_EMAIL = "auth.test@gea.edu.co";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        RolEntity rol = rolRepository.findByNombre("USUARIO_APP")
                .orElseGet(() -> {
                    RolEntity r = new RolEntity();
                    r.setNombre("USUARIO_APP");
                    return rolRepository.save(r);
                });

        Usuario usuario = new Usuario();
        usuario.setNombre("Test Auth");
        usuario.setCorreo(TEST_EMAIL);
        usuario.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        usuario.setRolEntity(rol);
        usuario.setEstado("ACTIVO");
        usuario.setAuthProvider(AuthProvider.LOCAL);
        usuarioRepository.save(usuario);
    }

    @Test
    void login_con_credenciales_correctas_retorna_token_valido() {
        AuthRequest request = new AuthRequest();
        request.setCorreo(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(jwtService.isTokenValid(response.getToken())).isTrue();
        assertThat(jwtService.extractUsername(response.getToken())).isEqualTo(TEST_EMAIL);
    }

    @Test
    void login_incluye_el_id_del_usuario_en_la_respuesta() {
        Usuario guardado = usuarioRepository.findAll().stream()
                .filter(x -> TEST_EMAIL.equals(x.getCorreo()))
                .findFirst().orElseThrow();

        AuthRequest request = new AuthRequest();
        request.setCorreo(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        AuthResponse response = authService.login(request);

        assertThat(response.getId()).isEqualTo(guardado.getId());
    }

    @Test
    void login_con_password_incorrecto_lanza_401() {
        AuthRequest request = new AuthRequest();
        request.setCorreo(TEST_EMAIL);
        request.setPassword("contraseña_incorrecta");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void login_con_correo_no_registrado_lanza_401() {
        AuthRequest request = new AuthRequest();
        request.setCorreo("noexiste@gea.edu.co");
        request.setPassword(TEST_PASSWORD);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void login_con_cuenta_inactiva_lanza_401() {
        Usuario u = usuarioRepository.findAll().stream()
                .filter(x -> TEST_EMAIL.equals(x.getCorreo()))
                .findFirst().orElseThrow();
        u.setEstado("INACTIVO");
        usuarioRepository.save(u);

        AuthRequest request = new AuthRequest();
        request.setCorreo(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }
}
