package com.rutina.service.calculo;

import java.util.List;

/**
 * Series generadas para una secuencia de pasos (zonas de un día, o días de una zona).
 *
 * @param porPaso tamaños de serie de cada paso (lista vacía = ese paso no lleva series)
 * @param desvio  Σ reps en series − Σ reps planificadas
 */
public record SeriesZona(List<List<Integer>> porPaso, int desvio) {
}
