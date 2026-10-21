package com.rutina.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CeldaPlantillaTest {

    @Test
    void parseCeldas() {
        assertEquals(new CeldaPlantilla(3, 5, 75, 80), CeldaPlantilla.parse("3x5-75/80%"));
        assertEquals(new CeldaPlantilla(3, 3, 90, 90), CeldaPlantilla.parse("3x3-90%"));
        assertEquals(new CeldaPlantilla(4, 12, null, null), CeldaPlantilla.parse("4x12"));
    }

    @Test
    void textoDevuelveElFormatoOriginal() {
        assertEquals("3x5-75/80%", CeldaPlantilla.parse("3x5-75/80%").texto());
        assertEquals("3x3-90%", CeldaPlantilla.parse("3x3-90%").texto());
        assertEquals("4x12", CeldaPlantilla.parse("4x12").texto());
    }

    @Test
    void textoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> CeldaPlantilla.parse("cuatro por doce"));
    }
}
