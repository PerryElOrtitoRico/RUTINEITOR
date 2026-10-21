package com.rutina.service.calculo;

import com.rutina.model.Ejercicio;
import com.rutina.model.Metodologia;
import com.rutina.model.Tipo;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PliometriaTest {

    private static final Metodologia M = DatosIniciales.metodologia();

    private static Ejercicio plioEj(String grupo, String nombre, int orden) {
        return new Ejercicio(0, 1, nombre, Tipo.PLIOMETRIA, null, null, 100, Map.of(), grupo, orden, true);
    }

    /** Grupos con n ejercicios cada uno: plio("1A", 6, "1B", 6). */
    private static List<Ejercicio> plio(Object... grupoYCantidad) {
        List<Ejercicio> res = new ArrayList<>();
        for (int i = 0; i < grupoYCantidad.length; i += 2) {
            String g = (String) grupoYCantidad[i];
            for (int k = 0; k < (int) grupoYCantidad[i + 1]; k++) res.add(plioEj(g, g + "-" + k, k));
        }
        return res;
    }

    private static List<Ejercicio> plioNombres(String grupo, String... nombres) {
        List<Ejercicio> res = new ArrayList<>();
        for (int k = 0; k < nombres.length; k++) res.add(plioEj(grupo, nombres[k], k));
        return res;
    }

    @Test
    void nivel1Semana1() {
        var s = Pliometria.generar(M, 1, plio("1A", 6, "1B", 6, "1C", 6)).get(0);
        assertEquals(List.of(250, 300, 250), s.stream().map(DiaPlio::contactos).toList());
        assertEquals(List.of(4, 5, 4), s.stream().map(DiaPlio::series).toList());
        assertEquals(6, s.get(1).ejercicios().size());
        assertEquals("1B-0", s.get(1).ejercicios().get(0));
    }

    @Test
    void nivel1Semana4() {
        assertEquals(List.of(6, 5, 6), Pliometria.generar(M, 1, plio("1A", 6, "1B", 6, "1C", 6)).get(3)
                .stream().map(DiaPlio::series).toList());
    }

    @Test
    void listaCortaSeRepite() {
        assertEquals(List.of("a", "b", "c", "d", "a", "b"),
                Pliometria.generar(M, 1, plioNombres("1A", "a", "b", "c", "d")).get(0).get(0).ejercicios());
    }

    @Test
    void listaLargaTomaLosPrimeros6() {
        assertEquals(List.of("a", "b", "c", "d", "e", "f"),
                Pliometria.generar(M, 1, plioNombres("1A", "a", "b", "c", "d", "e", "f", "g", "h")).get(0).get(0).ejercicios());
    }

    @Test
    void nivel3TieneDosDias() {
        var semanas = Pliometria.generar(M, 3, List.of());
        assertEquals(4, semanas.size());
        assertEquals(2, semanas.get(0).size());
        assertTrue(semanas.get(0).get(0).ejercicios().isEmpty());
    }
}
