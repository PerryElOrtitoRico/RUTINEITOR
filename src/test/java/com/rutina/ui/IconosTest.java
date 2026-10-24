package com.rutina.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IconosTest {

    @Test
    void losSeisTamaniosEstanEnElJar() {
        var iconos = VentanaPrincipal.iconos();
        assertEquals(6, iconos.size());
        assertEquals(256, iconos.get(5).getWidth(null));
    }
}
