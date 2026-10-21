package com.rutina.service.calculo;

import java.util.Map;

/** Reps de una zona asignadas a cada ejercicio, y las que ningún ejercicio pudo tomar por su techo. */
public record ResultadoReparto(Map<Long, Integer> porEjercicio, int sinAsignar) {
}
