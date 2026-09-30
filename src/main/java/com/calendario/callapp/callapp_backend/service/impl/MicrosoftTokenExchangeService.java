package com.calendario.callapp.callapp_backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Intercambia el código de autorización de Microsoft por un id_token,
 * servidor-a-servidor, usando el client secret.
 *
 * <p>El registro de la app "GeaApp" en Azure solo tiene el redirect URI
 * registrado bajo la plataforma Web (cliente confidencial) — sin
 * "Allow public client flows" habilitado en el portal, Microsoft rechaza
 * el intercambio de código sin client_secret. Este servicio existe
 * específicamente para eso: el secreto vive solo aquí (variable de
 * entorno, ver application.properties), nunca en la app móvil — un
 * secreto embebido en un APK es extraíble descompilando el instalador.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MicrosoftTokenExchangeService {

    private final RestTemplate restTemplate;

    @Value("${microsoft.tenant-id:}")
    private String tenantId;

    @Value("${microsoft.client-id:}")
    private String clientId;

    @Value("${microsoft.client-secret:}")
    private String clientSecret;

    @Value("${microsoft.redirect-uri:https://com.gea.app/oauth2redirect}")
    private String redirectUri;

    public String exchangeCodeForIdToken(String code) {
        String tokenEndpoint = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(
                    tokenEndpoint, new HttpEntity<>(body, headers), Map.class);
        } catch (HttpStatusCodeException ex) {
            // RestTemplate lanza excepción (no la envuelve en la respuesta) ante un
            // 4xx/5xx de Microsoft — típicamente invalid_grant (código expirado, ya
            // usado, o revocado). El cuerpo trae el detalle real, útil en el log,
            // pero no se expone tal cual al cliente.
            log.warn("Microsoft rechazó el intercambio de código: {} {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Microsoft rechazó el código de autorización", ex);
        }

        Object idToken = response.getBody() != null ? response.getBody().get("id_token") : null;
        if (idToken == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Microsoft no devolvió un id_token para este código");
        }
        return idToken.toString();
    }
}
