package com.rutina.service.calculo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneradorSeriesTest {

    @Test
    void arrastreCompensa() {
        var s = GeneradorSeries.generar(new int[]{4, 4, 4}, 3);
        assertEquals(List.of(List.of(3), List.of(3, 3), List.of(3)), s.porPaso());
        assertEquals(0, s.desvio());
    }

    @Test
    void pocasRepsSeAcumulanSinSeriesSueltas() {
        var s = GeneradorSeries.generar(new int[]{1, 0, 1}, 4);
        assertEquals(List.of(List.of(), List.of(), List.of(4)), s.porPaso());
        assertEquals(2, s.desvio());
    }

    @Test
    void restoChicoAlFinalSeDescarta() {
        var s = GeneradorSeries.generar(new int[]{1, 0, 0}, 4);
        assertEquals(List.of(List.of(), List.of(), List.of()), s.porPaso());
        assertEquals(-1, s.desvio());
    }

    @Test
    void arrastreNegativoNoGeneraSeries() {
        var s = GeneradorSeries.generar(new int[]{5, 0, 0}, 3);
        assertEquals(List.of(List.of(3, 3), List.of(), List.of()), s.porPaso());
        assertEquals(1, s.desvio());
    }

    @Test
    void sinReps() {
        var s = GeneradorSeries.generar(new int[]{0, 0}, 2);
        assertEquals(List.of(List.of(), List.of()), s.porPaso());
        assertEquals(0, s.desvio());
    }

    @Test
    void entreZonasDelMismoDia() { // 6 reps al 55 % y 6 al 60 % con series de 4: el sobrante del 55 % pasa al 60 %
        var s = GeneradorSeries.generar(new int[]{6, 6}, new int[]{4, 4});
        assertEquals(List.of(List.of(4, 4), List.of(4)), s.porPaso());
        assertEquals(0, s.desvio());
    }

    @Test
    void elRestoNoCreaSeriesEnZonasSinRepsPlanificadas() {
        var s = GeneradorSeries.generar(new int[]{0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, new int[]{4, 4, 4, 4, 3, 3, 3, 2, 1, 1});
        assertTrue(s.porPaso().stream().allMatch(List::isEmpty), s.toString());
        assertEquals(-1, s.desvio());
    }

    @Test
    void elRestoSaltaZonasVaciasHastaLaProximaPlanificada() {
        var s = GeneradorSeries.generar(new int[]{6, 0, 6}, new int[]{4, 4, 4});
        assertEquals(List.of(List.of(4, 4), List.of(), List.of(4)), s.porPaso());
    }

    @Test
    void notacion() {
        assertEquals("3.3.3.3", GeneradorSeries.notacion(List.of(3, 3, 3, 3)));
        assertEquals("", GeneradorSeries.notacion(List.of()));
    }
}
