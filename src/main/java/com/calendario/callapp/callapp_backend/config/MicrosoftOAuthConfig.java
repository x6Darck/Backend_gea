package com.calendario.callapp.callapp_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

/**
 * Bean singleton del {@link JwtDecoder} de Microsoft OIDC, con timeouts explícitos.
 *
 * <p>Antes se reconstruía en cada login ({@code JwtDecoders.fromIssuerLocation(...)}
 * dentro de {@code MicrosoftAuthServiceImpl.autenticar}), repitiendo el descubrimiento
 * OIDC (llamada de red sin timeout) por cada intento de login. Como bean, la
 * construcción ocurre una sola vez y Spring Security cachea internamente las claves
 * JWKS para los decodes siguientes, que pasan a ser una operación local de firma sin
 * red.</p>
 *
 * <p>{@code @Lazy} es obligatorio: {@code microsoft.tenant-id}/{@code client-id} son
 * opcionales (login Microsoft deshabilitado por defecto — no están configurados en
 * ningún entorno actual, ni test ni prod). {@code NimbusJwtDecoder.withIssuerLocation}
 * dispara el descubrimiento OIDC (llamada de red) en el momento de construir el bean,
 * no en el primer decode; sin {@code @Lazy}, un tenantId vacío rompe el arranque
 * completo de la aplicación al intentar descubrir un emisor con URL inválida
 * (verificado: sin esta anotación, toda la suite de tests con {@code @SpringBootTest}
 * falla al cargar el contexto). Con {@code @Lazy}, la construcción se difiere al
 * primer login real.</p>
 */
@Configuration
public class MicrosoftOAuthConfig {

    @Bean
    @Lazy
    public JwtDecoder microsoftJwtDecoder(@Value("${microsoft.tenant-id:}") String tenantId) {
        String issuer = "https://login.microsoftonline.com/" + tenantId + "/v2.0";

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);

        return NimbusJwtDecoder.withIssuerLocation(issuer)
                .restOperations(new RestTemplate(requestFactory))
                .build();
    }

    /**
     * Usado por {@link com.calendario.callapp.callapp_backend.service.impl.MicrosoftTokenExchangeService}
     * para el intercambio código→token con el endpoint de Microsoft. No es
     * {@code @Lazy}: a diferencia del JwtDecoder, construir un RestTemplate no
     * hace ninguna llamada de red, así que no hay riesgo de romper el arranque.
     */
    @Bean
    public RestTemplate microsoftRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        return new RestTemplate(requestFactory);
    }
}
