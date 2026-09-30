package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.request.AuthRequest;
import com.calendario.callapp.callapp_backend.dto.request.MicrosoftAuthCodeRequest;
import com.calendario.callapp.callapp_backend.dto.request.MicrosoftAuthRequest;
import com.calendario.callapp.callapp_backend.dto.response.AuthResponse;
import com.calendario.callapp.callapp_backend.security.JwtService;
import com.calendario.callapp.callapp_backend.security.TokenBlacklistService;
import com.calendario.callapp.callapp_backend.service.AuthService;
import com.calendario.callapp.callapp_backend.service.impl.MicrosoftAuthServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
/**
 * Endpoints de autenticación: login local, login Microsoft OAuth2 y logout.
 *
 * <p>En login exitoso emite el JWT como cookie {@code gea_auth} (HttpOnly, Secure, SameSite=None)
 * para clientes web y también lo incluye en el cuerpo del {@link AuthResponse} para clientes móviles.
 * En logout añade el JTI del token a la lista negra de {@link com.calendario.callapp.callapp_backend.security.TokenBlacklistService}
 * y borra la cookie.</p>
 */
public class AuthController {

    private final AuthService authService;
    private final MicrosoftAuthServiceImpl microsoftAuthService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.expiration}")
    private long tokenExpiration;

    @Value("${app.security.cookie.name:gea_auth}")
    private String cookieName;

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletResponse httpResponse) {

        AuthResponse authResponse = authService.login(request);

        // Cookie HttpOnly para clientes web (el token también va en el body para la app móvil)
        ResponseCookie cookie = ResponseCookie.from(cookieName, authResponse.getToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(tokenExpiration / 1000)
                .sameSite(cookieSameSite)
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Sesión iniciada correctamente"));
    }

    @PostMapping("/microsoft/mobile")
    public ResponseEntity<ApiResponse<AuthResponse>> microsoftMobile(@Valid @RequestBody MicrosoftAuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success(microsoftAuthService.autenticar(request), "Sesión de Microsoft iniciada"));
    }

    // Para apps registradas en Azure como cliente confidencial (sin "Allow
    // public client flows") — recibe el código de autorización en vez del
    // idToken directo; el intercambio con client_secret ocurre en el backend.
    @PostMapping("/microsoft/mobile/code")
    public ResponseEntity<ApiResponse<AuthResponse>> microsoftMobileConCodigo(@Valid @RequestBody MicrosoftAuthCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(microsoftAuthService.autenticarConCodigo(request), "Sesión de Microsoft iniciada"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse httpResponse) {

        // Revocar desde Authorization header (móvil / Bearer)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            revokeToken(authHeader.substring(7));
        }

        // Revocar desde cookie (web)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    revokeToken(cookie.getValue());
                    break;
                }
            }
        }

        // Limpiar la cookie en el cliente
        ResponseCookie clearCookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

        return ResponseEntity.ok(ApiResponse.success(null, "Sesión cerrada correctamente"));
    }

    private void revokeToken(String token) {
        String jti = jwtService.extractJti(token);
        Date expiration = jwtService.extractExpiration(token);
        if (jti != null && expiration != null) {
            tokenBlacklistService.blacklist(jti, expiration);
        }
    }
}
