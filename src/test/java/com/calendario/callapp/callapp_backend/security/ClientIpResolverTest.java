package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private ClientIpResolver resolverConProxy(boolean trustProxy) {
        ClientIpResolver r = new ClientIpResolver();
        ReflectionTestUtils.setField(r, "trustProxy", trustProxy);
        return r;
    }

    @Test
    void con_trust_proxy_toma_el_ultimo_salto_de_x_forwarded_for() {
        // El cliente puede falsear los saltos anteriores; Nginx agrega el real al FINAL.
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "1.1.1.1, 200.10.20.30");
        req.setRemoteAddr("127.0.0.1");

        assertThat(resolverConProxy(true).resolve((HttpServletRequest) req)).isEqualTo("200.10.20.30");
    }

    @Test
    void sin_trust_proxy_ignora_el_header_y_usa_remote_addr() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "1.1.1.1");
        req.setRemoteAddr("10.0.0.5");

        assertThat(resolverConProxy(false).resolve((HttpServletRequest) req)).isEqualTo("10.0.0.5");
    }

    @Test
    void con_x_forwarded_for_con_basura_cae_a_remote_addr() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "no-es-una-ip");
        req.setRemoteAddr("10.0.0.9");

        assertThat(resolverConProxy(true).resolve((HttpServletRequest) req)).isEqualTo("10.0.0.9");
    }
}
