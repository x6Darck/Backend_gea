package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.MicrosoftAuthCodeRequest;
import com.calendario.callapp.callapp_backend.dto.request.MicrosoftAuthRequest;
import com.calendario.callapp.callapp_backend.dto.response.AuthResponse;
import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.MotivoFalloLogin;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Slf4j
/**
 * Servicio de autenticación mediante Microsoft OAuth2 (PKCE) para la app móvil.
 *
 * <p>Recibe el {@code idToken} obtenido por la app Flutter tras el flujo PKCE con
 * Microsoft, lo valida mediante el emisor {@code https://login.microsoftonline.com/{tenantId}/v2.0},
 * verifica la audiencia ({@code clientId}) y auto-provisiona un {@link com.calendario.callapp.callapp_backend.entity.Usuario}
 * con rol {@code Usuario Autenticado} si no existe. Requiere las propiedades
 * {@code microsoft.tenant-id} y {@code microsoft.client-id} configuradas.</p>
 */
public class MicrosoftAuthServiceImpl {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final JwtService jwtService;
    private final JwtDecoder microsoftJwtDecoder;
    private final MicrosoftTokenExchangeService microsoftTokenExchangeService;
    private final HistorialLoginService historialLoginService;

    // Constructor explícito (no @RequiredArgsConstructor de Lombok): @Lazy debe ir en
    // el parámetro del constructor para que Spring difiera la construcción real del
    // JwtDecoder al primer uso; Lombok no copia anotaciones arbitrarias a los
    // parámetros del constructor que genera salvo que se configure explícitamente en
    // lombok.config, así que colocarlo solo en el campo (como se intentó primero) es
    // inerte y no evita el arranque fallido con microsoft.tenant-id vacío.
    public MicrosoftAuthServiceImpl(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            JwtService jwtService,
            @Lazy JwtDecoder microsoftJwtDecoder,
            MicrosoftTokenExchangeService microsoftTokenExchangeService,
            HistorialLoginService historialLoginService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.jwtService = jwtService;
        this.microsoftJwtDecoder = microsoftJwtDecoder;
        this.microsoftTokenExchangeService = microsoftTokenExchangeService;
        this.historialLoginService = historialLoginService;
    }

    @Value("${microsoft.tenant-id:}")
    private String tenantId = "";

    @Value("${microsoft.client-id:}")
    private String clientId = "";

    // Placeholder usado al registrar un fallo de login en el historial cuando
    // el correo del usuario aún no se ha podido resolver del JWT en ese punto
    // del flujo (token ilegible, audiencia incorrecta, o falta el claim del
    // que se obtiene el correo).
    private static final String CORREO_DESCONOCIDO = "desconocido";

    @Transactional
    public AuthResponse autenticar(MicrosoftAuthRequest request) {
        verificarConfigurado();

        Jwt jwt;
        try {
            jwt = microsoftJwtDecoder.decode(request.getIdToken());
        } catch (JwtException ex) {
            historialLoginService.registrarFallo(CORREO_DESCONOCIDO, null, AuthProvider.MICROSOFT, MotivoFalloLogin.TOKEN_INVALIDO);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Microsoft invalido");
        }

        return procesarJwtValidado(jwt);
    }

    /**
     * Igual que {@link #autenticar}, pero para el registro de la app en Azure
     * que solo admite cliente confidencial (sin "Allow public client flows"):
     * recibe el código de autorización en vez del idToken directo, y lo
     * cambia por un id_token vía {@link MicrosoftTokenExchangeService} (con
     * el client_secret, que nunca sale del backend) antes de validar.
     */
    @Transactional
    public AuthResponse autenticarConCodigo(MicrosoftAuthCodeRequest request) {
        verificarConfigurado();

        String idToken = microsoftTokenExchangeService.exchangeCodeForIdToken(request.getCode());

        Jwt jwt;
        try {
            jwt = microsoftJwtDecoder.decode(idToken);
        } catch (JwtException ex) {
            historialLoginService.registrarFallo(CORREO_DESCONOCIDO, null, AuthProvider.MICROSOFT, MotivoFalloLogin.TOKEN_INVALIDO);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Microsoft invalido");
        }

        return procesarJwtValidado(jwt);
    }

    private void verificarConfigurado() {
        if (tenantId == null || tenantId.isBlank() || clientId == null || clientId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_IMPLEMENTED,
                    "Configura microsoft.tenant-id y microsoft.client-id para habilitar este login"
            );
        }
    }

    private AuthResponse procesarJwtValidado(Jwt jwt) {
        validarAudiencia(jwt);

        String correo = obtenerClaim(jwt, "preferred_username", null);
        String nombre = obtenerClaim(jwt, "name", correo);
        String oid = obtenerClaim(jwt, "oid", correo);

        Usuario usuario = usuarioRepository.getByCorreoOptimized(correo).orElseGet(Usuario::new);
        if (usuario.getId() == null) {
            log.info("Creando nuevo usuario desde Microsoft OAuth: {}", correo);
            usuario.setCorreo(correo);
            usuario.setNombre(nombre != null && !nombre.isBlank() ? nombre : correo);
            usuario.setPassword(UUID.randomUUID().toString());
            usuario.setEstado("ACTIVO");
            usuario.setFechaCreacion(java.time.LocalDateTime.now());
            usuario.setRolEntity(rolRepository.findByNombre("Usuario Autenticado").orElse(null));
            usuario.setAuthProvider(AuthProvider.MICROSOFT);
        } else {
            if (!"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
                log.warn("Login Microsoft rechazado - cuenta inactiva: {}", correo);
                historialLoginService.registrarFallo(correo, usuario.getId(), AuthProvider.MICROSOFT, MotivoFalloLogin.CUENTA_INACTIVA);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tu cuenta está inactiva. Contacta al administrador.");
            }
            log.info("Login Microsoft exitoso: {}", correo);
        }

        usuario.setMicrosoftOid(oid);
        usuario.setAuthProvider(AuthProvider.MICROSOFT);
        if (usuario.getRolEntity() == null) {
            usuario.setRolEntity(rolRepository.findByNombre("Usuario Autenticado").orElse(null));
        }

        Usuario guardado;
        try {
            guardado = usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Otro request concurrente creó el mismo usuario: re-leer por correo
            guardado = usuarioRepository.getByCorreoOptimized(correo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Conflicto al crear el usuario, reintente"));
        }

        com.calendario.callapp.callapp_backend.entity.Oficina oficina = guardado.getOficina();
        Long idOficina = oficina != null ? oficina.getId() : null;
        String oficinaNombre = oficina != null ? oficina.getNombre() : null;

        historialLoginService.registrarExito(guardado.getCorreo(), guardado.getId(), AuthProvider.MICROSOFT);

        return AuthResponse.builder()
                .id(guardado.getId())
                .token(jwtService.generarToken(guardado))
                .nombre(guardado.getNombre())
                .correo(guardado.getCorreo())
                .rol(guardado.getRol().name())
                .idOficina(idOficina)
                .oficinaNombre(oficinaNombre)
                .fotoUrl(guardado.getFotoUrl())
                .build();
    }

    private void validarAudiencia(Jwt jwt) {
        if (jwt.getAudience() == null || jwt.getAudience().stream().noneMatch(clientId::equals)) {
            historialLoginService.registrarFallo(CORREO_DESCONOCIDO, null, AuthProvider.MICROSOFT, MotivoFalloLogin.TOKEN_INVALIDO);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token no pertenece a esta aplicacion");
        }
    }

    private String obtenerClaim(Jwt jwt, String claim, String correoConocido) {
        Object value = jwt.getClaims().get(claim);
        if (value == null) {
            String correoLog = (correoConocido != null && !correoConocido.isBlank()) ? correoConocido : CORREO_DESCONOCIDO;
            historialLoginService.registrarFallo(correoLog, null, AuthProvider.MICROSOFT, MotivoFalloLogin.TOKEN_INVALIDO);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta el claim requerido: " + claim);
        }
        return value.toString();
    }
}
