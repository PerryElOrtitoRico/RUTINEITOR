package com.rutina.registro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T01: no se le muestra al LLM. */
class T01EscalasOcultoTest {

    @Test
    void descansoCompleto() {
        int[] valores = {6, 5, 4, 3, 2, 1};
        String[] textos = {"Bien descansado y con ganas de entrenar", "Bien descansado", "Bien descansado pero distraído",
                "Mal descansado pero con ganas de entrenar", "Mal descansado", "Mal descansado y sin ganas de entrenar"};
        assertEquals(6, EscalaDescanso.values().length);
        for (int i = 0; i < 6; i++) {
            assertEquals(valores[i], EscalaDescanso.values()[i].valor());
            assertEquals(textos[i], EscalaDescanso.values()[i].texto());
            assertSame(EscalaDescanso.values()[i], EscalaDescanso.deValor(valores[i]));
        }
    }

    @Test
    void esfuerzoYFinal() {
        assertEquals(5, EscalaEsfuerzo.values().length);
        assertEquals("Al límite", EscalaEsfuerzo.deValor(5).texto());
        assertEquals("Muy liviano", EscalaEsfuerzo.deValor(1).texto());
        assertEquals(5, EscalaFinal.values().length);
        assertEquals("Con energía", EscalaFinal.deValor(5).texto());
        assertEquals("Destruido", EscalaFinal.deValor(1).texto());
        assertEquals(3, EscalaFinal.NORMAL.valor());
    }

    @Test
    void fueraDeRangoConMensaje() {
        var e = assertThrows(IllegalArgumentException.class, () -> EscalaFinal.deValor(9));
        assertTrue(e.getMessage().contains("9"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> EscalaDescanso.deValor(7));
        assertThrows(IllegalArgumentException.class, () -> EscalaEsfuerzo.deValor(0));
        assertThrows(IllegalArgumentException.class, () -> EscalaEsfuerzo.deValor(-1));
    }
}
