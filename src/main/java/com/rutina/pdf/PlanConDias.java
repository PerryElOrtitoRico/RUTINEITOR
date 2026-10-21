package com.rutina.pdf;

import com.rutina.model.PlanPliometria;
import com.rutina.service.calculo.DiaPlio;

import java.util.List;
import java.util.Set;

/**
 * Un plan de pliometría listo para imprimir.
 *
 * @param semanas                   [semana][día], de {@code Pliometria.generar}
 * @param semanasDentroDelMesociclo semanas del plan (desde 1) que caen dentro del mesociclo
 */
public record PlanConDias(PlanPliometria plan, List<List<DiaPlio>> semanas, Set<Integer> semanasDentroDelMesociclo) {
}
