package com.calendario.callapp.callapp_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
/**
 * Define el bean {@link BCryptPasswordEncoder} para encriptado de contraseñas.
 *
 * <p>Separado de {@link com.calendario.callapp.callapp_backend.security.SecurityConfig}
 * para evitar dependencias circulares: {@code UsuarioServiceImpl} necesita el encoder
 * sin importar toda la configuración de seguridad.</p>
 */
public class PasswordConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
