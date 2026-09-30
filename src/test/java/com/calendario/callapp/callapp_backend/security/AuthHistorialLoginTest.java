package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.dto.request.AuthRequest;
import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.MotivoFalloLogin;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NO @Transactional: el registro usa REQUIRES_NEW y debe commitear de verdad;
 * limpieza manual del historial por test.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthHistorialLoginTest {

    @Autowired private AuthServiceImpl authService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HistorialLoginRepository historialRepository;

    private static final String PASS = "password123";
    private String correo;

    @BeforeEach
    void setUp() {
        historialRepository.deleteAll();
        correo = "hist.login." + System.nanoTime() + "@gea.edu.co";
        RolEntity rol = rolRepository.findByNombre("USUARIO_APP")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("USUARIO_APP"); return rolRepository.save(r); });
        Usuario u = new Usuario();
        u.setNombre("Hist Login");
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode(PASS));
        u.setRolEntity(rol);
        u.setEstado("ACTIVO");
        u.setAuthProvider(AuthProvider.LOCAL);
        usuarioRepository.save(u);
    }

    @Test
    void login_exitoso_registra_fila_exito() {
        AuthRequest req = new AuthRequest();
        req.setCorreo(correo);
        req.setPassword(PASS);

        authService.login(req);

        var filas = historialRepository.findAll();
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getExito()).isTrue();
        assertThat(filas.get(0).getMetodo()).isEqualTo(AuthProvider.LOCAL);
        assertThat(filas.get(0).getIdUsuario()).isNotNull();
    }

    @Test
    void login_password_incorrecto_registra_fila_fallo() {
        AuthRequest req = new AuthRequest();
        req.setCorreo(correo);
        req.setPassword("mala");

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(RuntimeException.class);

        var filas = historialRepository.findAll();
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getExito()).isFalse();
        assertThat(filas.get(0).getMotivoFallo()).isEqualTo(MotivoFalloLogin.CREDENCIALES_INVALIDAS);
    }

    @Test
    void login_cuenta_inactiva_registra_fila_fallo() {
        Usuario u = usuarioRepository.findAll().stream()
                .filter(x -> correo.equals(x.getCorreo()))
                .findFirst().orElseThrow();
        u.setEstado("INACTIVO");
        usuarioRepository.save(u);

        AuthRequest req = new AuthRequest();
        req.setCorreo(correo);
        req.setPassword(PASS);

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(RuntimeException.class);

        var filas = historialRepository.findAll();
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getExito()).isFalse();
        assertThat(filas.get(0).getMotivoFallo()).isEqualTo(MotivoFalloLogin.CUENTA_INACTIVA);
        assertThat(filas.get(0).getIdUsuario()).isNotNull();
    }

    @Test
    void login_correo_inexistente_registra_fila_fallo() {
        AuthRequest req = new AuthRequest();
        req.setCorreo("no.existe." + System.nanoTime() + "@gea.edu.co");
        req.setPassword(PASS);

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(RuntimeException.class);

        var filas = historialRepository.findAll();
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getExito()).isFalse();
        assertThat(filas.get(0).getMotivoFallo()).isEqualTo(MotivoFalloLogin.CORREO_NO_REGISTRADO);
    }
}
