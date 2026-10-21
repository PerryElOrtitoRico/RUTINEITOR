package com.rutina.model;

import java.util.List;

/**
 * Plantilla de fuerza básica o hipertrofia. {@code semanas.get(w).get(k)} es la celda de la
 * aparición k del ejercicio en la semana w. {@code inferida} indica que la generó el programa.
 */
public record Plantilla(List<List<CeldaPlantilla>> semanas, boolean inferida) {
}
