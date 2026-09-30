package com.calendario.callapp.callapp_backend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El registro de la app en Azure solo tiene el redirect URI bajo la
 * plataforma Web (cliente confidencial) — sin "Allow public client flows"
 * habilitado, Microsoft exige client_secret en el intercambio del código
 * por el token. Este servicio hace ese intercambio server-to-server; el
 * secreto nunca sale del backend.
 */
class MicrosoftTokenExchangeServiceTest {

    private RestTemplate restTemplate;
    private MicrosoftTokenExchangeService service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        service = new MicrosoftTokenExchangeService(restTemplate);
        ReflectionTestUtils.setField(service, "tenantId", "tenant-123");
        ReflectionTestUtils.setField(service, "clientId", "client-abc");
        ReflectionTestUtils.setField(service, "clientSecret", "secreto-de-prueba");
        ReflectionTestUtils.setField(service, "redirectUri", "https://com.gea.app/oauth2redirect");
    }

    @Test
    void intercambiaElCodePorElIdTokenUsandoElClientSecret() {
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> respuesta = mock(ResponseEntity.class);
        when(respuesta.getBody()).thenReturn(Map.of("id_token", "el-id-token-real", "access_token", "at"));
        when(restTemplate.postForEntity(
                eq("https://login.microsoftonline.com/tenant-123/oauth2/v2.0/token"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(respuesta);

        String idToken = service.exchangeCodeForIdToken("codigo-de-autorizacion");

        assertThat(idToken).isEqualTo("el-id-token-real");
    }

    @Test
    void envuiaClientIdSecretCodeYRedirectUriEnElCuerpoDeLaPeticion() {
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> respuesta = mock(ResponseEntity.class);
        when(respuesta.getBody()).thenReturn(Map.of("id_token", "x"));

        var captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(any(String.class), captor.capture(), eq(Map.class)))
                .thenReturn(respuesta);

        service.exchangeCodeForIdToken("codigo-de-autorizacion");

        @SuppressWarnings("unchecked")
        MultiValueMap<String, String> body = (MultiValueMap<String, String>) captor.getValue().getBody();
        assertThat(body.getFirst("client_id")).isEqualTo("client-abc");
        assertThat(body.getFirst("client_secret")).isEqualTo("secreto-de-prueba");
        assertThat(body.getFirst("code")).isEqualTo("codigo-de-autorizacion");
        assertThat(body.getFirst("redirect_uri")).isEqualTo("https://com.gea.app/oauth2redirect");
        assertThat(body.getFirst("grant_type")).isEqualTo("authorization_code");

        HttpHeaders headers = captor.getValue().getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
    }

    @Test
    void lanza401SiLaRespuestaDeMicrosoftNoTraeIdToken() {
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> respuesta = mock(ResponseEntity.class);
        when(respuesta.getBody()).thenReturn(Map.of("access_token", "solo-access-sin-id-token"));
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(respuesta);

        assertThatThrownBy(() -> service.exchangeCodeForIdToken("codigo"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("id_token");
    }

    @Test
    void convierteElRechazoDeMicrosoftEn401EnVezDePropagarLaExcepcionCruda() {
        // RestTemplate lanza HttpClientErrorException (no la devuelve envuelta en la
        // respuesta) cuando Microsoft responde 4xx — ej. código de autorización
        // inválido o expirado (invalid_grant). Verificado contra el error real de
        // Microsoft: sin capturarla, se propaga como 500 genérico en vez de un 401
        // claro para el cliente.
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request",
                        null, "{\"error\":\"invalid_grant\"}".getBytes(), null));

        assertThatThrownBy(() -> service.exchangeCodeForIdToken("codigo-invalido-o-expirado"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Microsoft rechazó el código");
    }
}
