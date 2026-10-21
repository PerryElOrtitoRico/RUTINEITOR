package com.rutina.model;

import java.util.Map;

/**
 * Ejercicio de un perfil.
 *
 * @param rm           1RM actual en kg (null si no se usa con carga medida)
 * @param techoPct     zona más alta que puede recibir en la aproximación compleja (100 por defecto)
 * @param repsPorSerie solo las zonas que el usuario pisó; las demás heredan del tipo
 * @param grupoPlio    grupo de pliometría ("1A", "2C"…); null en los demás tipos
 * @param orden        posición dentro de su lista
 */
public record Ejercicio(long id, long perfilId, String nombre, Tipo tipo, Double rm, Double rmObjetivo,
                        int techoPct, Map<Zona, Integer> repsPorSerie, String grupoPlio, int orden,
                        boolean activo) {

    public int repsPorSerie(Zona z, Metodologia m) {
        Integer propio = repsPorSerie == null ? null : repsPorSerie.get(z);
        if (propio != null) return propio;
        int[] delTipo = m.repsPorSerie().getOrDefault(tipo, m.repsPorSerie().get(Tipo.FUERZA));
        return delTipo[z.ordinal()];
    }
}
