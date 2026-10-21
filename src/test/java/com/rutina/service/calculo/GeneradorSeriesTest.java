package com.rutina.service.calculo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneradorSeriesTest {

    @Test
    void arrastreCompensa() {
        var s = GeneradorSeries.generar(new int[]{4, 4, 4}, 3);
        assertEquals(List.of(List.of(3), List.of(3, 3), List.of(3)), s.porDia());
        assertEquals(0, s.desvio());
    }

    @Test
    void pocasRepsSeAcumulanSinSeriesSueltas() {
        var s = GeneradorSeries.generar(new int[]{1, 0, 1}, 4);
        assertEquals(List.of(List.of(), List.of(), List.of(4)), s.porDia());
        assertEquals(2, s.desvio());
    }

    @Test
    void restoChicoAlFinalSeDescarta() {
        var s = GeneradorSeries.generar(new int[]{1, 0, 0}, 4);
        assertEquals(List.of(List.of(), List.of(), List.of()), s.porDia());
        assertEquals(-1, s.desvio());
    }

    @Test
    void arrastreNegativoNoGeneraSeries() {
        var s = GeneradorSeries.generar(new int[]{5, 0, 0}, 3);
        assertEquals(List.of(List.of(3, 3), List.of(), List.of()), s.porDia());
        assertEquals(1, s.desvio());
    }

    @Test
    void sinReps() {
        var s = GeneradorSeries.generar(new int[]{0, 0}, 2);
        assertEquals(List.of(List.of(), List.of()), s.porDia());
        assertEquals(0, s.desvio());
    }

    @Test
    void notacion() {
        assertEquals("3.3.3.3", GeneradorSeries.notacion(List.of(3, 3, 3, 3)));
        assertEquals("", GeneradorSeries.notacion(List.of()));
    }
}
