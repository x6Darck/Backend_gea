package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.util.ClientIpResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La clave del rate limiter no debe depender solo de la IP: en el NAT de un
 * campus universitario, cientos de usuarios comparten una IP pública y
 * terminan compartiendo la misma cuota (429 falsos a usuarios legítimos).
 * Cuando la petición trae un JWT válido, la clave debe ser por-usuario.
 */
class RateLimiterFilterTest {

    private JwtService jwtService;
    private ClientIpResolver clientIpResolver;
    private RateLimiterFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "test_secret_minimo_32_caracteres_x123");
        ReflectionTestUtils.setField(jwtService, "tokenExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtService, "springEnvironment", new MockEnvironment());

        clientIpResolver = new ClientIpResolver();
        filter = new RateLimiterFilter(jwtService, clientIpResolver);
        ReflectionTestUtils.setField(filter, "cookieName", "gea_auth");
    }

    private String tokenPara(String correo) {
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        RolEntity rolEntity = new RolEntity();
        rolEntity.setNombre("OFICINA");
        usuario.setRolEntity(rolEntity);
        return jwtService.generarToken(usuario);
    }

    @Test
    void usaClavePorUsuarioCuandoElBearerTraeUnJwtValido() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenPara("ana@unilibre.edu.co"));

        String key = filter.resolveRateLimitKey(request);

        assertEquals("user:ana@unilibre.edu.co", key);
    }

    @Test
    void usaClavePorUsuarioCuandoLaCookieTraeUnJwtValido() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("gea_auth", tokenPara("luis@unilibre.edu.co")) });

        String key = filter.resolveRateLimitKey(request);

        assertEquals("user:luis@unilibre.edu.co", key);
    }

    @Test
    void caeAClavePorIpCuandoNoHayTokenPresente() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");

        String key = filter.resolveRateLimitKey(request);

        assertEquals("ip:10.0.0.5", key);
    }

    @Test
    void caeAClavePorIpCuandoElTokenEsInvalido() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-basura-no-es-un-jwt");
        when(request.getRemoteAddr()).thenReturn("10.0.0.9");

        String key = filter.resolveRateLimitKey(request);

        assertEquals("ip:10.0.0.9", key);
    }

    @Test
    void distintosUsuariosCompartiendoIpObtienenClavesDistintas() {
        HttpServletRequest requestA = mock(HttpServletRequest.class);
        when(requestA.getHeader("Authorization")).thenReturn("Bearer " + tokenPara("estudianteA@unilibre.edu.co"));

        HttpServletRequest requestB = mock(HttpServletRequest.class);
        when(requestB.getHeader("Authorization")).thenReturn("Bearer " + tokenPara("estudianteB@unilibre.edu.co"));

        String keyA = filter.resolveRateLimitKey(requestA);
        String keyB = filter.resolveRateLimitKey(requestB);

        assertEquals("user:estudianteA@unilibre.edu.co", keyA);
        assertEquals("user:estudianteB@unilibre.edu.co", keyB);
    }

    /**
     * Con trust-proxy=true confiamos en UN solo salto (Nginx, Tarea 3.1),
     * que agrega su propio remote_addr al final de X-Forwarded-For vía
     * $proxy_add_x_forwarded_for — nunca lo antepone. La posición confiable
     * es siempre la ÚLTIMA (lo que Nginx observó directamente), no la
     * primera: un cliente puede enviar su propio X-Forwarded-For con
     * cualquier valor falso, que Nginx conserva intacto delante del suyo.
     * Leer la primera posición permite a cualquiera falsificar su IP y
     * eludir el rate-limiter por completo.
     */
    @Nested
    class ResolucionDeIpConProxyConfiable {

        private HttpServletRequest requestConHeaders(String xForwardedFor, String remoteAddr) {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
            when(request.getRemoteAddr()).thenReturn(remoteAddr);
            return request;
        }

        @org.junit.jupiter.api.BeforeEach
        void confiarEnElProxy() {
            ReflectionTestUtils.setField(clientIpResolver, "trustProxy", true);
        }

        @Test
        void usaLaUltimaIpDeLaCadenaCuandoNginxAgregoLaSuya() {
            // Nginx vio a un cliente real en 190.10.20.30 y agregó esa IP al
            // final (su remote_addr real, no falsificable desde fuera).
            HttpServletRequest request = requestConHeaders("190.10.20.30", "172.19.0.5");

            String key = filter.resolveRateLimitKey(request);

            assertEquals("ip:190.10.20.30", key);
        }

        @Test
        void ignoraUnXForwardedForFalsificadoPorElClienteYUsaElQueAgregoNginx() {
            // Un cliente malicioso manda su propio X-Forwarded-For; Nginx lo
            // conserva delante del suyo, pero el valor real (no falsificable)
            // es el que Nginx agregó al final de la cadena.
            HttpServletRequest request = requestConHeaders("6.6.6.6, 190.10.20.30", "172.19.0.5");

            String key = filter.resolveRateLimitKey(request);

            assertEquals("ip:190.10.20.30", key);
        }

        @Test
        void caeAlRemoteAddrSiNoHayXForwardedFor() {
            HttpServletRequest request = requestConHeaders(null, "172.19.0.5");

            String key = filter.resolveRateLimitKey(request);

            assertEquals("ip:172.19.0.5", key);
        }
    }

    /**
     * El límite ya no es un único valor con un multiplicador fijo x5: login,
     * tráfico general/autenticado y rutas públicas de solo lectura tienen
     * cupos independientes, calibrados para que un NAT de campus compartido
     * (cientos de usuarios legítimos tras una sola IP) no colapse contra un
     * límite pensado originalmente para un solo usuario.
     */
    @Nested
    class LimiteSegunRuta {

        @org.junit.jupiter.api.BeforeEach
        void configurarLimites() {
            ReflectionTestUtils.setField(filter, "maxRequests", 150);
            ReflectionTestUtils.setField(filter, "nonAuthMultiplier", 10);
            ReflectionTestUtils.setField(filter, "authRequestsPerMinute", 150);
        }

        @Test
        void rutaDeLoginUsaElLimiteDedicadoDeAuth() {
            assertEquals(150, filter.resolveMaxRequests("/auth/login"));
        }

        @Test
        void rutaDeLoginConPrefijoApiUsaElLimiteDedicadoDeAuth() {
            assertEquals(150, filter.resolveMaxRequests("/api/auth/login"));
        }

        @Test
        void rutaGeneralUsaElLimiteBaseMultiplicado() {
            assertEquals(1500, filter.resolveMaxRequests("/app/eventos/publicados"));
        }

        @Test
        void rutaDeEscrituraAutenticadaTambienUsaElLimiteMultiplicado() {
            assertEquals(1500, filter.resolveMaxRequests("/oficina/solicitudes-evento"));
        }

        @Test
        void elLimiteDeAuthEsIndependienteDelMultiplicadorGeneral() {
            ReflectionTestUtils.setField(filter, "authRequestsPerMinute", 80);
            assertEquals(80, filter.resolveMaxRequests("/auth/login"));
            assertEquals(1500, filter.resolveMaxRequests("/app/anuncios/publicados"));
        }
    }
}
