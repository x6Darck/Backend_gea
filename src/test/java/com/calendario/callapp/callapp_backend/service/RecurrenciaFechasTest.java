package com.calendario.callapp.callapp_backend.service;

import com.calendario.callapp.callapp_backend.entity.FrecuenciaRecurrencia;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudEventoServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecurrenciaFechasTest {

    @Test
    void diariaTresDiasGeneraDosInstanciasSecundarias() {
        LocalDate inicio = LocalDate.of(2026, 6, 24);
        List<LocalDate> fechas = SolicitudEventoServiceImpl.calcularFechasRecurrencia(
            inicio, FrecuenciaRecurrencia.DIARIA, LocalDate.of(2026, 6, 26), 100);
        assertEquals(List.of(LocalDate.of(2026, 6, 25), LocalDate.of(2026, 6, 26)), fechas);
    }

    @Test
    void respetaLimiteMaximoDeInstancias() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        List<LocalDate> fechas = SolicitudEventoServiceImpl.calcularFechasRecurrencia(
            inicio, FrecuenciaRecurrencia.DIARIA, LocalDate.of(2030, 1, 1), 100);
        assertEquals(100, fechas.size());
    }

    @Test
    void sinFechasCuandoFinEsAntesDelSiguientePaso() {
        LocalDate inicio = LocalDate.of(2026, 6, 24);
        List<LocalDate> fechas = SolicitudEventoServiceImpl.calcularFechasRecurrencia(
            inicio, FrecuenciaRecurrencia.SEMANAL, LocalDate.of(2026, 6, 26), 100);
        assertEquals(List.of(), fechas);
    }
}
