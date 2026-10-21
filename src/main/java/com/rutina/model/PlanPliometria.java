package com.rutina.model;

import java.time.LocalDate;

/** Plan de pliometría: independiente del mesociclo, siempre dura 4 semanas. */
public record PlanPliometria(long id, long perfilId, int nivel, LocalDate inicio) {
}
