package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Colaborador interno de {@link HistorialLoginService}: aísla el único punto
 * donde ocurre el {@code REQUIRES_NEW} real. Al vivir en un bean separado, la
 * apertura y el commit de esa transacción (que Spring gestiona ALREDEDOR de la
 * invocación, no dentro del cuerpo del método) quedan cubiertos por el
 * try/catch de {@code HistorialLoginService} — si esta clase tuviera el
 * {@code @Transactional} en el mismo método que hace el try/catch, un fallo al
 * abrir o comitear la transacción escaparía sin capturar, porque esas dos
 * operaciones las hace el proxy de Spring FUERA del cuerpo del método
 * interceptado. Separar en dos beans obliga a esa llamada a pasar por un proxy
 * distinto, que el try/catch del llamador sí envuelve por completo.
 */
@Component
@RequiredArgsConstructor
class HistorialLoginPersister {

    private final HistorialLoginRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarEnNuevaTransaccion(HistorialLogin h) {
        repository.save(h);
    }
}
