package com.rutina.service.calculo;

import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DistribucionVolumenTest {

    private static final Metodologia M = DatosIniciales.metodologia();

    @Test
    void fuerza5A() throws Exception {
        assertEquals(List.of(new PlanMicro(74, 76), new PlanMicro(80, 75), new PlanMicro(91, 74),
                        new PlanMicro(63, 78), new PlanMicro(42, 79)),
                DistribucionVolumen.porMicrociclo(M, new ParametrosTipo(350, 76), 5, 'A'));
    }

    @Test
    void imrFueraDeTabla() {
        var e = assertThrows(ParametrosInvalidosException.class,
                () -> DistribucionVolumen.porMicrociclo(M, new ParametrosTipo(350, 79), 5, 'A'));
        assertTrue(e.getMessage().contains("82"), e.getMessage());
        assertTrue(e.getMessage().contains("microciclo 5"), e.getMessage());
    }

    @Test
    void varianteInexistente() {
        var e = assertThrows(ParametrosInvalidosException.class,
                () -> DistribucionVolumen.porMicrociclo(M, new ParametrosTipo(300, 72), 5, 'C'));
        assertTrue(e.getMessage().contains("5C"), e.getMessage());
    }

    @Test
    void pesosDiasReescalados() {
        assertArrayEquals(new double[]{0.35 / 0.55, 0, 0.20 / 0.55},
                DistribucionVolumen.pesosDias(M, 3, Set.of(1, 3)), 1e-9);
    }

    @Test
    void unSoloDiaSeLlevaTodo() {
        assertArrayEquals(new double[]{0, 0, 1, 0}, DistribucionVolumen.pesosDias(M, 4, Set.of(3)), 1e-9);
    }

    @Test
    void sinDiasTodoCero() {
        assertArrayEquals(new double[]{0, 0, 0}, DistribucionVolumen.pesosDias(M, 3, Set.of()), 1e-9);
    }
}
