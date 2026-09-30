package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import com.calendario.callapp.callapp_backend.util.ClientIpResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Prueba unitaria (sin contexto Spring) del contrato "nunca lanza" de
 * {@link HistorialLoginService}. Vive en el paquete {@code service.impl}
 * porque necesita instanciar directamente {@link HistorialLoginPersister},
 * que es package-private a propósito (ver su Javadoc): es un colaborador
 * interno, no una API pública del módulo.
 *
 * <p>Aquí se fuerza a que el persister (el bean donde realmente ocurre el
 * {@code REQUIRES_NEW}) explote, simulando exactamente el escenario que
 * motivó separar la persistencia en su propio bean: un fallo al abrir o
 * comitear esa transacción (ej. agotamiento del pool de conexiones durante
 * un flood de intentos de login). El servicio debe absorberlo y no propagar
 * nada al llamador.</p>
 */
class HistorialLoginServiceUnitTest {

    @Test
    void registrar_nunca_propaga_una_excepcion_del_persister() {
        HistorialLoginPersister persisterQueFalla = mock(HistorialLoginPersister.class);
        doThrow(new RuntimeException("fallo simulado de BD")).when(persisterQueFalla).guardarEnNuevaTransaccion(any());
        HistorialLoginRepository repoNoUsado = mock(HistorialLoginRepository.class);
        ClientIpResolver resolver = new ClientIpResolver();

        HistorialLoginService service = new HistorialLoginService(persisterQueFalla, repoNoUsado, resolver);

        assertThatCode(() -> service.registrarExito("test@gea.edu.co", 1L, AuthProvider.LOCAL))
                .doesNotThrowAnyException();
    }
}
