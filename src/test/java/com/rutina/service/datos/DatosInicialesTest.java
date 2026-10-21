package com.rutina.service.datos;

import com.rutina.model.ClavePlantilla;
import com.rutina.model.Metodologia;
import com.rutina.model.Tipo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DatosInicialesTest {

    private static final Metodologia M = DatosIniciales.metodologia();

    @Test
    void variantesSuman100() {
        M.variantes().forEach((k, v) -> assertEquals(100.0, Arrays.stream(v.volPct()).sum(), 1e-9, k));
    }

    @Test
    void hayDiezVariantes() {
        assertEquals(Set.of("5A", "5B", "4A", "4B", "4C", "3A", "3B", "3C", "2A", "2B"), M.variantes().keySet());
    }

    @Test
    void deltaImr4A() {
        assertArrayEquals(new int[]{0, 2, -3, 3}, M.variantes().get("4A").deltaImr());
    }

    @Test
    void diasSuman1() {
        M.diaPct().forEach((k, v) -> assertEquals(1.0, Arrays.stream(v).sum(), 1e-9, "días " + k));
    }

    @Test
    void refFilas65y66Corregidas() {
        var r = M.ref();
        assertEquals(21, r.size());
        assertEquals(100.0, Arrays.stream(r.get(65)).sum(), 1e-9);
        assertEquals(100.0, Arrays.stream(r.get(66)).sum(), 1e-9);
        assertEquals(120.0, Arrays.stream(r.get(60)).sum(), 1e-9); // errata del libro: se conserva
    }

    @Test
    void repsPorSeriePorDefecto() {
        assertArrayEquals(new int[]{4, 4, 4, 4, 3, 3, 3, 2, 1, 1}, M.repsPorSerie().get(Tipo.FUERZA));
        assertArrayEquals(new int[]{4, 4, 4, 4, 3, 3, 3, 2, 1, 1}, M.repsPorSerie().get(Tipo.POTENCIA));
    }

    @Test
    void plantillaFuerza3Semanas2Dias() {
        var p = M.plantillas().get(new ClavePlantilla(Tipo.FUERZA, 3, 2));
        assertEquals("3x6-70/75%", p.semanas().get(1).get(0).texto());
        assertFalse(p.inferida());
    }

    @Test
    void plantillaHipertrofia5Semanas1Dia() {
        var p = M.plantillas().get(new ClavePlantilla(Tipo.HIPERTROFIA, 5, 1));
        assertEquals("4x8", p.semanas().get(4).get(0).texto());
    }

    @Test
    void volPlioNivel3() {
        assertArrayEquals(new int[]{350, 350}, M.volPlio().get(3)[3]);
        assertArrayEquals(new int[]{250, 300, 250}, M.volPlio().get(1)[0]);
    }

    @Test
    void incrementoKg() {
        assertEquals(1.0, M.incrementoKg());
    }
}
