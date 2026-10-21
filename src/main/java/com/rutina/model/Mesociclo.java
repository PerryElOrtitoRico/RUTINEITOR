package com.rutina.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Mesociclo planificado en el macrociclo y, una vez armado, su grilla.
 *
 * @param variante       A/B/C; null hasta que el usuario la elige al armar
 * @param dias           días por microciclo (3–5); null hasta que se elige
 * @param aproximaciones aproximación de cada ejercicio de fuerza (por id de ejercicio)
 * @param rmUsados       1RM con los que se exportó por última vez (vacío si nunca)
 */
public record Mesociclo(long id, long perfilId, LocalDate inicio, int microciclos,
                        ParametrosTipo potencia, ParametrosTipo fuerza,
                        Character variante, Integer dias,
                        Map<Long, Aproximacion> aproximaciones, List<Bloque> bloques,
                        String calentamiento, String auxiliares, Map<Long, Double> rmUsados) {

    /** Lunes siguiente al último microciclo. */
    public LocalDate fin() {
        return inicio.plusWeeks(microciclos);
    }
}
