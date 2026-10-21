package com.rutina;

import com.formdev.flatlaf.FlatLightLaf;
import com.rutina.ui.VentanaPrincipal;

import javax.swing.SwingUtilities;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(VentanaPrincipal::iniciar);
    }
}
