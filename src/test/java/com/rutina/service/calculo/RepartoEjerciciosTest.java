package com.rutina.service.calculo;

import com.rutina.model.Zona;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RepartoEjerciciosTest {

    @Test
    void techoPasaAlOtro() {
        var r = RepartoEjercicios.repartir(10, Zona.Z90,
                List.of(new Participante(1, 50, 100), new Participante(2, 50, 85)));
        assertEquals(Map.of(1L, 10), r.porEjercicio());
        assertEquals(0, r.sinAsignar());
    }

    @Test
    void mitadYMitad() {
        assertEquals(Map.of(1L, 5, 2L, 4), RepartoEjercicios.repartir(9, Zona.Z60,
                List.of(new Participante(1, 50, 100), new Participante(2, 50, 100))).porEjercicio());
    }

    @Test
    void nadieAdmiteLaZona() {
        var r = RepartoEjercicios.repartir(4, Zona.Z95, List.of(new Participante(2, 100, 85)));
        assertEquals(4, r.sinAsignar());
        assertTrue(r.porEjercicio().isEmpty());
    }

    @Test
    void sinParticipantes() {
        assertEquals(7, RepartoEjercicios.repartir(7, Zona.Z55, List.of()).sinAsignar());
    }

    @Test
    void porcentajesQueNoSuman100SeNormalizan() {
        assertEquals(Map.of(1L, 6, 2L, 3), RepartoEjercicios.repartir(9, Zona.Z60,
                List.of(new Participante(1, 60, 100), new Participante(2, 30, 100))).porEjercicio());
    }
}
