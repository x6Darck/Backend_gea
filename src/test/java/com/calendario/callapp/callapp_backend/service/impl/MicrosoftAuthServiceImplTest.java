package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.MicrosoftAuthCodeRequest;
import com.calendario.callapp.callapp_backend.dto.request.MicrosoftAuthRequest;
import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.MotivoFalloLogin;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El registro "GeaApp" en Azure solo admite cliente confidencial (requiere
 * client_secret) — {@code autenticarConCodigo} recibe el código de
 * autorización, lo cambia por un id_token vía
 * {@link MicrosoftTokenExchangeService} (el secreto nunca sale del backend),
 * y valida ese id_token igual que el flujo previo basado en idToken directo.
 */
class MicrosoftAuthServiceImplTest {

    private UsuarioRepository usuarioRepository;
    private RolRepository rolRepository;
    private JwtService jwtService;
    private JwtDecoder microsoftJwtDecoder;
    private MicrosoftTokenExchangeService tokenExchangeService;
    private HistorialLoginService historialLoginService;
    private MicrosoftAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        rolRepository = mock(RolRepository.class);
        jwtService = mock(JwtService.class);
        microsoftJwtDecoder = mock(JwtDecoder.class);
        tokenExchangeService = mock(MicrosoftTokenExchangeService.class);
        historialLoginService = mock(HistorialLoginService.class);

        service = new MicrosoftAuthServiceImpl(
                usuarioRepository, rolRepository, jwtService, microsoftJwtDecoder, tokenExchangeService,
                historialLoginService);
        ReflectionTestUtils.setField(service, "tenantId", "tenant-123");
        ReflectionTestUtils.setField(service, "clientId", "client-abc");
    }

    private Jwt jwtValido() {
        return Jwt.withTokenValue("token-valor")
                .header("alg", "RS256")
                .audience(List.of("client-abc"))
                .claim("preferred_username", "ana@unilibre.edu.co")
                .claim("name", "Ana Pérez")
                .claim("oid", "oid-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void autenticarConCodigoIntercambiaElCodePorIdTokenYCreaUsuarioNuevo() {
        when(tokenExchangeService.exchangeCodeForIdToken("codigo-autorizacion")).thenReturn("el-id-token");
        when(microsoftJwtDecoder.decode("el-id-token")).thenReturn(jwtValido());
        when(usuarioRepository.getByCorreoOptimized("ana@unilibre.edu.co")).thenReturn(Optional.empty());
        RolEntity rolUsuarioAutenticado = new RolEntity();
        rolUsuarioAutenticado.setNombre("Usuario Autenticado");
        when(rolRepository.findByNombre("Usuario Autenticado")).thenReturn(Optional.of(rolUsuarioAutenticado));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(jwtService.generarToken(any(Usuario.class))).thenReturn("jwt-de-gea");

        MicrosoftAuthCodeRequest request = new MicrosoftAuthCodeRequest();
        request.setCode("codigo-autorizacion");

        var respuesta = service.autenticarConCodigo(request);

        assertThat(respuesta.getToken()).isEqualTo("jwt-de-gea");
        verify(usuarioRepository).save(any(Usuario.class));
        verify(historialLoginService).registrarExito(
                eq("ana@unilibre.edu.co"), eq(99L), eq(AuthProvider.MICROSOFT));
    }

    @Test
    void autenticarConCodigoRechazaSiElIdTokenNoPerteneceAEstaApp() {
        when(tokenExchangeService.exchangeCodeForIdToken("codigo-autorizacion")).thenReturn("el-id-token");
        Jwt jwtDeOtraApp = Jwt.withTokenValue("token-valor")
                .header("alg", "RS256")
                .audience(List.of("otra-app-distinta"))
                .claim("preferred_username", "ana@unilibre.edu.co")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(microsoftJwtDecoder.decode("el-id-token")).thenReturn(jwtDeOtraApp);

        MicrosoftAuthCodeRequest request = new MicrosoftAuthCodeRequest();
        request.setCode("codigo-autorizacion");

        assertThatThrownBy(() -> service.autenticarConCodigo(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no pertenece a esta aplicacion");

        verify(historialLoginService).registrarFallo(
                eq("desconocido"), eq(null), eq(AuthProvider.MICROSOFT), eq(MotivoFalloLogin.TOKEN_INVALIDO));
    }

    @Test
    void autenticarConCodigoDevuelve501SiMicrosoftNoEstaConfigurado() {
        ReflectionTestUtils.setField(service, "tenantId", "");
        ReflectionTestUtils.setField(service, "clientId", "");

        MicrosoftAuthCodeRequest request = new MicrosoftAuthCodeRequest();
        request.setCode("codigo-autorizacion");

        assertThatThrownBy(() -> service.autenticarConCodigo(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Configura microsoft.tenant-id");
    }

    @Test
    void autenticarConCodigoRegistraFalloSiElIdTokenEsInvalido() {
        when(tokenExchangeService.exchangeCodeForIdToken("codigo-autorizacion")).thenReturn("el-id-token");
        when(microsoftJwtDecoder.decode("el-id-token")).thenThrow(new JwtException("firma invalida"));

        MicrosoftAuthCodeRequest request = new MicrosoftAuthCodeRequest();
        request.setCode("codigo-autorizacion");

        assertThatThrownBy(() -> service.autenticarConCodigo(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Token de Microsoft invalido");

        verify(historialLoginService).registrarFallo(
                eq("desconocido"), eq(null), eq(AuthProvider.MICROSOFT), eq(MotivoFalloLogin.TOKEN_INVALIDO));
    }

    @Test
    void autenticarRegistraFalloSiElIdTokenEsInvalido() {
        when(microsoftJwtDecoder.decode("token-directo")).thenThrow(new JwtException("token expirado"));

        MicrosoftAuthRequest request = new MicrosoftAuthRequest();
        request.setIdToken("token-directo");

        assertThatThrownBy(() -> service.autenticar(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Token de Microsoft invalido");

        verify(historialLoginService).registrarFallo(
                eq("desconocido"), eq(null), eq(AuthProvider.MICROSOFT), eq(MotivoFalloLogin.TOKEN_INVALIDO));
    }

    @Test
    void autenticarConCodigoRechazaCuentaInactiva() {
        when(tokenExchangeService.exchangeCodeForIdToken("codigo-autorizacion")).thenReturn("el-id-token");
        when(microsoftJwtDecoder.decode("el-id-token")).thenReturn(jwtValido());

        Usuario usuarioInactivo = new Usuario();
        usuarioInactivo.setId(42L);
        usuarioInactivo.setCorreo("ana@unilibre.edu.co");
        usuarioInactivo.setEstado("INACTIVO");
        when(usuarioRepository.getByCorreoOptimized("ana@unilibre.edu.co"))
                .thenReturn(Optional.of(usuarioInactivo));

        MicrosoftAuthCodeRequest request = new MicrosoftAuthCodeRequest();
        request.setCode("codigo-autorizacion");

        assertThatThrownBy(() -> service.autenticarConCodigo(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inactiva");

        verify(historialLoginService).registrarFallo(
                eq("ana@unilibre.edu.co"), eq(42L), eq(AuthProvider.MICROSOFT), eq(MotivoFalloLogin.CUENTA_INACTIVA));
    }

    @Test
    void autenticarConCodigoRegistraFalloConCorreoDesconocidoSiFaltaElClaimDeCorreo() {
        when(tokenExchangeService.exchangeCodeForIdToken("codigo-autorizacion")).thenReturn("el-id-token");
        Jwt jwtSinCorreo = Jwt.withTokenValue("token-valor")
                .header("alg", "RS256")
                .audience(List.of("client-abc"))
                .claim("name", "Ana Pérez")
                .claim("oid", "oid-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(microsoftJwtDecoder.decode("el-id-token")).thenReturn(jwtSinCorreo);

        MicrosoftAuthCodeRequest request = new MicrosoftAuthCodeRequest();
        request.setCode("codigo-autorizacion");

        assertThatThrownBy(() -> service.autenticarConCodigo(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Falta el claim requerido: preferred_username");

        verify(historialLoginService).registrarFallo(
                eq("desconocido"), eq(null), eq(AuthProvider.MICROSOFT), eq(MotivoFalloLogin.TOKEN_INVALIDO));
    }

    @Test
    void autenticarConCodigoRegistraFalloConCorreoConocidoSiFaltaUnClaimPosteriorAlCorreo() {
        when(tokenExchangeService.exchangeCodeForIdToken("codigo-autorizacion")).thenReturn("el-id-token");
        Jwt jwtSinOid = Jwt.withTokenValue("token-valor")
                .header("alg", "RS256")
                .audience(List.of("client-abc"))
                .claim("preferred_username", "ana@unilibre.edu.co")
                .claim("name", "Ana Pérez")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(microsoftJwtDecoder.decode("el-id-token")).thenReturn(jwtSinOid);

        MicrosoftAuthCodeRequest request = new MicrosoftAuthCodeRequest();
        request.setCode("codigo-autorizacion");

        assertThatThrownBy(() -> service.autenticarConCodigo(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Falta el claim requerido: oid");

        verify(historialLoginService).registrarFallo(
                eq("ana@unilibre.edu.co"), eq(null), eq(AuthProvider.MICROSOFT), eq(MotivoFalloLogin.TOKEN_INVALIDO));
    }
}
