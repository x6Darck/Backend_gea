package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.security.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Caffeine cache behavior in CustomUserDetailsService.
 * NOTE: No @Transactional — cache semantics require real DB commits
 * to observe divergence between cache and database state.
 */
@SpringBootTest
@ActiveProfiles("test")
class UserCacheTest {

    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private CacheManager cacheManager;

    private static final String CACHE_EMAIL = "cache.test@gea.edu.co";

    @BeforeEach
    void setUp() {
        // Evict cache before each test
        var cache = cacheManager.getCache("userDetails");
        if (cache != null) cache.clear();

        // Clean up any leftover user from prior test
        usuarioRepository.findAll().stream()
                .filter(u -> CACHE_EMAIL.equals(u.getCorreo()))
                .forEach(usuarioRepository::delete);

        RolEntity rol = rolRepository.findByNombre("USUARIO_APP")
                .orElseGet(() -> {
                    RolEntity r = new RolEntity();
                    r.setNombre("USUARIO_APP");
                    return rolRepository.save(r);
                });

        Usuario u = new Usuario();
        u.setNombre("Cache Test User");
        u.setCorreo(CACHE_EMAIL);
        u.setPassword("dummy");
        u.setRolEntity(rol);
        u.setEstado("ACTIVO");
        u.setAuthProvider(AuthProvider.LOCAL);
        usuarioRepository.save(u);
    }

    @AfterEach
    void tearDown() {
        // Evict cache and remove test user to keep state clean between tests
        userDetailsService.invalidarCacheUsuario(CACHE_EMAIL);
        usuarioRepository.findAll().stream()
                .filter(u -> CACHE_EMAIL.equals(u.getCorreo()))
                .forEach(usuarioRepository::delete);
    }

    @Test
    void carga_inicial_retorna_usuario_activo() {
        UserDetails details = userDetailsService.loadUserByUsername(CACHE_EMAIL);

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo(CACHE_EMAIL);
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void segunda_carga_sin_eviccion_usa_cache_y_no_refleja_cambio_en_bd() {
        // First load fills cache with ACTIVO state
        userDetailsService.loadUserByUsername(CACHE_EMAIL);

        // Change state directly in DB without going through the service (no eviction)
        usuarioRepository.findAll().stream()
                .filter(u -> CACHE_EMAIL.equals(u.getCorreo()))
                .findFirst().ifPresent(u -> {
                    u.setEstado("INACTIVO");
                    usuarioRepository.save(u);
                });

        // Second load → must return cached value (ACTIVO), not DB (INACTIVO)
        UserDetails details = userDetailsService.loadUserByUsername(CACHE_EMAIL);
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void despues_de_eviccion_refleja_estado_actualizado_de_bd() {
        // First load fills cache
        userDetailsService.loadUserByUsername(CACHE_EMAIL);

        // Change state in DB
        usuarioRepository.findAll().stream()
                .filter(u -> CACHE_EMAIL.equals(u.getCorreo()))
                .findFirst().ifPresent(u -> {
                    u.setEstado("INACTIVO");
                    usuarioRepository.save(u);
                });

        // Evict cache entry
        userDetailsService.invalidarCacheUsuario(CACHE_EMAIL);

        // Third load → must go to DB and return INACTIVO (disabled)
        UserDetails details = userDetailsService.loadUserByUsername(CACHE_EMAIL);
        assertThat(details.isEnabled()).isFalse();
    }
}
