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
    void ejerciciosIniciales() {
        var e = DatosIniciales.ejercicios();
        assertEquals(32, e.stream().filter(x -> x.tipo() != Tipo.PLIOMETRIA).count());
        assertEquals(18, e.stream().filter(x -> x.tipo() == Tipo.PLIOMETRIA).count());
        var cargada = e.stream().filter(x -> x.nombre().equals("Cargada de potencia colgado")).findFirst().get();
        assertEquals(115.0, cargada.rm());
        assertEquals(120.0, cargada.rmObjetivo());
        assertEquals(Tipo.POTENCIA, cargada.tipo());
        assertEquals(Tipo.FUERZA, e.stream().filter(x -> x.nombre().equals("Sentadilla")).findFirst().get().tipo());
        assertEquals(Tipo.HIPERTROFIA, e.stream().filter(x -> x.nombre().equals("Triceps frontal")).findFirst().get().tipo());
        assertNull(e.stream().filter(x -> x.nombre().equals("Remo acostado")).findFirst().get().rm());
        assertEquals(6, e.stream().filter(x -> "1C".equals(x.grupoPlio())).count());
    }

    @Test
    void repsPorSerieHeredaDelTipo() {
        var e = new com.rutina.model.Ejercicio(1, 1, "X", Tipo.FUERZA, 100.0, null, 100,
                java.util.Map.of(com.rutina.model.Zona.Z80, 5), null, 0, true);
        assertEquals(5, e.repsPorSerie(com.rutina.model.Zona.Z80, M));
        assertEquals(2, e.repsPorSerie(com.rutina.model.Zona.Z90, M));
    }

    @Test
    void incrementoKg() {
        assertEquals(2.5, M.incrementoKg()); // discos de 1,25 kg por lado
    }
}
