package com.rutina.model;

/** Identifica una plantilla: tipo, semanas del mesociclo y veces por semana que aparece el ejercicio. */
public record ClavePlantilla(Tipo tipo, int semanas, int diasPorSemana) {
}
