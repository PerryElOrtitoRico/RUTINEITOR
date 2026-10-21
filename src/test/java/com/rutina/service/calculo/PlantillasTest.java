package com.rutina.service.calculo;

import com.rutina.model.CeldaPlantilla;
import com.rutina.model.Metodologia;
import com.rutina.model.Tipo;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlantillasTest {

    private static final Metodologia M = DatosIniciales.metodologia();

    private static List<String> textos(List<CeldaPlantilla> semana) {
        return semana.stream().map(CeldaPlantilla::texto).toList();
    }

    @Test
    void fuerza3Dias() {
        var p = Plantillas.obtener(M, Tipo.FUERZA, 3, 3);
        assertEquals(List.of("3x5-75/80%", "3x3-90%", "3x6-70/75%"), textos(p.semanas().get(0)));
        assertTrue(p.inferida());
    }

    @Test
    void hipertrofia3DiasSemana2() {
        assertEquals(List.of("4x10", "3x8", "4x12"), textos(Plantillas.obtener(M, Tipo.HIPERTROFIA, 2, 3).semanas().get(1)));
    }

    @Test
    void cincoDiasUnSoloPesado() {
        var sem = Plantillas.obtener(M, Tipo.FUERZA, 4, 5).semanas().get(0);
        assertEquals(5, sem.size());
        assertEquals(1, sem.stream().filter(c -> c.equals(sem.get(1))).count());
    }

    @Test
    void cuatroDias() {
        assertEquals(List.of("3x6-70/75%", "3x4-80/85%", "3x7-65/70%", "3x6-70/75%"),
                textos(Plantillas.obtener(M, Tipo.FUERZA, 4, 4).semanas().get(0)));
    }

    @Test
    void unDiaNoEsInferida() {
        assertFalse(Plantillas.obtener(M, Tipo.HIPERTROFIA, 5, 1).inferida());
    }

    @Test
    void plantillaInexistente() {
        assertThrows(IllegalArgumentException.class, () -> Plantillas.obtener(M, Tipo.FUERZA, 6, 2));
    }
}
