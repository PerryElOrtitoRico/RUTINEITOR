package com.rutina.registro;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T02. */
class T02NotacionOcultoTest {

    private static final List<SerieReal> PLAN = List.of(new SerieReal(3, 120), new SerieReal(3, 120), new SerieReal(3, 127.5));

    private static List<SerieReal> p(String t) {
        return NotacionRegistro.parse(t, PLAN);
    }

    @Test
    void ejemplosDeLaEspecificacion() {
        assertEquals(List.of(new SerieReal(3, 120), new SerieReal(3, 85), new SerieReal(3, 80)), p("3.3-85.3-80"));
        assertEquals(List.of(new SerieReal(3, 120), new SerieReal(3, 120)), p("3.3"));
        assertEquals(List.of(new SerieReal(2, 120), new SerieReal(3, 120)), p("2.3"));
        assertEquals(List.of(new SerieReal(3, 82.5)), p("3-82,5"));
        assertEquals(List.of(), p("0"));
    }

    @Test
    void pesoPorPosicionYUltimo() {
        // la 3.ª serie toma el peso de la 3.ª planificada (127,5); la 4.ª, el de la última
        assertEquals(List.of(new SerieReal(3, 120), new SerieReal(3, 120), new SerieReal(3, 127.5), new SerieReal(2, 127.5)),
                p("3.3.3.2"));
    }

    @Test
    void puntoSeparaSeries() {
        assertEquals(List.of(new SerieReal(3, 80), new SerieReal(5, 120)), p("3-80.5"));
    }

    @Test
    void espaciosIgnorados() {
        assertEquals(List.of(new SerieReal(3, 120), new SerieReal(3, 85)), p(" 3 . 3 - 85 "));
        assertEquals(List.of(), p(" 0 "));
    }

    @Test
    void decimalesYLimites() {
        assertEquals(List.of(new SerieReal(99, 120)), p("99"));
        assertEquals(List.of(new SerieReal(1, 0.5)), p("1-0,5"));
        assertEquals(List.of(new SerieReal(10, 200)), p("10-200"));
    }

    @Test
    void sinPlanificadas() {
        var e = assertThrows(IllegalArgumentException.class, () -> NotacionRegistro.parse("3", List.of()));
        assertTrue(e.getMessage().contains("indicá el peso"), e.getMessage());
        assertEquals(List.of(new SerieReal(3, 90)), NotacionRegistro.parse("3-90", List.of()));
        assertEquals(List.of(), NotacionRegistro.parse("0", List.of()));
    }

    @Test
    void invalidos() {
        for (String t : new String[]{"", "   ", "3..3", "3-", "-80", "0.3", "3.0", "100", "3-0", "a", "3.", ".3", "3-8a",
                "3--80", "3-80,", "3-,5", "+3", "3,5", "00", "3-80,5,1"}) {
            assertThrows(IllegalArgumentException.class, () -> p(t), "debería ser inválido: [" + t + "]");
            assertFalse(NotacionRegistro.esValida(t, PLAN), "esValida debería ser false: [" + t + "]");
        }
    }

    @Test
    void nullEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> NotacionRegistro.parse(null, PLAN));
        assertFalse(NotacionRegistro.esValida(null, PLAN));
    }

    @Test
    void validos() {
        assertTrue(NotacionRegistro.esValida("3.3-85.3-80", PLAN));
        assertTrue(NotacionRegistro.esValida("0", PLAN));
    }

    @Test
    void noModificaLaListaPlanificada() {
        var plan = new java.util.ArrayList<>(PLAN);
        NotacionRegistro.parse("3.3.3.3", plan);
        assertEquals(PLAN, plan);
    }
}
