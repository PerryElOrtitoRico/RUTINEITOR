package com.rutina.service.calculo;

import com.rutina.model.Metodologia;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistribucionZonasTest {

    private static final Metodologia M = DatosIniciales.metodologia();

    @Test
    void ejemploLibro() {
        assertArrayEquals(new int[]{13, 16, 18, 20, 21, 36, 29, 10, 0, 0}, DistribucionZonas.repsPorZona(M, 163, 74));
    }

    @Test
    void fila60SeNormaliza() {
        assertArrayEquals(new int[]{46, 33, 13, 8, 0, 0, 0, 0, 0, 0}, DistribucionZonas.repsPorZona(M, 100, 60));
    }

    @Test
    void repartirEnDiasSumaExacto() {
        assertArrayEquals(new int[]{5, 0, 3}, DistribucionZonas.repartirEnDias(8, new double[]{0.35 / 0.55, 0, 0.20 / 0.55}));
    }
}
