package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Implementación de {@link UserDetailsService} con caché Caffeine.
 *
 * <p>Carga el usuario por correo desde la BD y lo convierte a {@link org.springframework.security.core.userdetails.UserDetails}
 * con la autoridad {@code ROLE_<nombre-del-rol>}. Los resultados se cachean en {@code "userDetails"}
 * con TTL de 45 segundos (definido en {@link CacheConfig}): el tiempo máximo que tarda
 * en bloquear el acceso a un usuario desactivado.</p>
 */
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Cacheable(value = "userDetails", key = "#correo")
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.getByCorreoOptimized(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getPassword())
                .disabled(!"ACTIVO".equalsIgnoreCase(usuario.getEstado()))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getSecurityRole())))
                .build();
    }

    // Llamar cuando se cambia estado/rol de un usuario para invalidar el cache inmediatamente
    @CacheEvict(value = "userDetails", key = "#correo")
    public void invalidarCacheUsuario(String correo) {
        // Spring maneja la evicción automáticamente con la anotación
    }
}
