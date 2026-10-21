package com.rutina.model;

import java.time.LocalDate;

/** Un valor del historial de 1RM. */
public record PuntoRm(LocalDate fecha, double rm) {
}
