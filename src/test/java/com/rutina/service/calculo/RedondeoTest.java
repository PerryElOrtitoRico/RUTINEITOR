package com.rutina.service.calculo;

import com.rutina.model.Zona;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedondeoTest {

    @Test
    void mayorRestoEjemploLibro() { // 163 reps, fila IMR 74 de Ref
        assertArrayEquals(new int[]{13, 16, 18, 20, 21, 36, 29, 10},
                Redondeo.mayorResto(163, new double[]{8, 10, 11, 12, 13, 22, 18, 6}));
    }

    @Test
    void mayorRestoEmpateGanaIndiceMenor() {
        assertArrayEquals(new int[]{74, 80, 91, 63, 42},
                Redondeo.mayorResto(350, new double[]{21, 23, 26, 18, 12}));
    }

    @Test
    void mayorRestoPesosCero() {
        assertArrayEquals(new int[]{0, 0}, Redondeo.mayorResto(10, new double[]{0, 0}));
    }

    @Test
    void kgRedondea() {
        assertEquals(88.0, Redondeo.kg(100, 87.5, 1.0));
        assertEquals(92.5, Redondeo.kg(115, 80, 2.5));
    }

    @Test
    void halfUp() {
        assertEquals(3, Redondeo.halfUp(2.5));
        assertEquals(4, Redondeo.halfUp(250 / 60.0));
        assertEquals(6, Redondeo.halfUp(350 / 60.0));
    }

    @Test
    void zonasDePct() {
        assertEquals(10, Zona.values().length);
        assertEquals(Zona.Z85, Zona.dePct(85));
        assertThrows(IllegalArgumentException.class, () -> Zona.dePct(87));
    }
}
