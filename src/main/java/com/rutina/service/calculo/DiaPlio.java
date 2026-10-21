package com.rutina.service.calculo;

import java.util.List;

/** Un día de pliometría: cada ejercicio se hace {@code series} × {@link Pliometria#SALTOS_POR_SERIE}. */
public record DiaPlio(int contactos, List<String> ejercicios, int series) {
}
