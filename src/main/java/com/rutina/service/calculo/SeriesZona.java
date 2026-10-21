package com.rutina.service.calculo;

import java.util.List;

/**
 * Series de un ejercicio en una zona a lo largo de los días del microciclo.
 *
 * @param porDia tamaños de serie de cada día (lista vacía = no se trabaja esa zona ese día)
 * @param desvio Σ reps en series − Σ reps planificadas
 */
public record SeriesZona(List<List<Integer>> porDia, int desvio) {
}
