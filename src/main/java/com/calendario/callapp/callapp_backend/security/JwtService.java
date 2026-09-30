package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
/**
 * Generación y validación de tokens JWT con firma HMAC-SHA256.
 *
 * <p>Cada token incluye un identificador único ({@code jti} = UUID v4) para
 * soporte de lista negra en logout, y lleva el rol del usuario como claim
 * {@code rol}. La clave secreta se valida al inicio ({@code @PostConstruct}):
 * si no está configurada o tiene menos de 32 caracteres, la aplicación no arranca.
 * En modo test la validación de firma se relaja para facilitar pruebas unitarias.</p>
 */
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long tokenExpiration;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment springEnvironment;

    @org.springframework.beans.factory.annotation.Autowired
    private TokenBlacklistService tokenBlacklistService;

    @PostConstruct
    public void validarConfiguracion() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                "FATAL: jwt.secret no está configurado. Define la variable de entorno JWT_SECRET.");
        }
        if (secretKey.length() < 32) {
            throw new IllegalStateException(
                "FATAL: jwt.secret debe tener al menos 32 caracteres. Longitud actual: " + secretKey.length());
        }
        if (secretKey.startsWith("dev_only_") && isProductionEnvironment()) {
            throw new IllegalStateException(
                "FATAL: Estás usando el JWT secret de desarrollo en un entorno de producción. " +
                "Define JWT_SECRET con un valor seguro antes de arrancar.");
        }
        log.info("Configuración JWT validada correctamente.");
        log.debug("JWT secret length: {} chars.", secretKey.length());
    }

    private boolean isProductionEnvironment() {
        String[] profiles = springEnvironment.getActiveProfiles();
        if (profiles.length == 0) return false;
        for (String p : profiles) {
            if (p.contains("prod")) return true;
        }
        for (String p : profiles) {
            if (p.contains("dev")) return false;
        }
        return true;
    }

    public String generarToken(Usuario usuario) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(usuario.getCorreo())
                .claim("id", usuario.getId())
                .claim("rol", usuario.getRol().getSecurityRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRol(String token) {
        return extractClaim(token, claims -> claims.get("rol", String.class));
    }

    public boolean isTokenValid(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        if (expiration == null || !expiration.after(new Date())) return false;
        String jti = extractClaim(token, Claims::getId);
        // Tokens sin JTI (generados antes de esta versión) no pueden ser revocados pero siguen siendo válidos
        return jti == null || !tokenBlacklistService.isBlacklisted(jti);
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claimsResolver.apply(claims);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("JWT con firma inválida detectado");
            return null;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return null;
        } catch (Exception e) {
            log.debug("JWT inválido o malformado: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
