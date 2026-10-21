package com.rutina.service.calculo;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.CeldaPlantilla;
import com.rutina.model.Ejercicio;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;

import java.util.List;
import java.util.Map;

/** Todo lo que se muestra del mesociclo calculado: series por bloque, contadores y datos de los gráficos. */
public record ResultadoMesociclo(List<ResultadoBloque> bloques, List<Contador> contadores,
                                 List<PuntoGrafico> planificado, List<PuntoGrafico> real, List<String> avisos) {

    /**
     * Un microciclo de un bloque.
     *
     * @param volumen       reps reales del bloque en el microciclo
     * @param seriesPorZona compleja: para cada zona, una lista con las series de este bloque (vacía si no la trabaja)
     * @param celda         básica o hipertrofia: la celda de la plantilla; null en compleja
     */
    public record SemanaBloque(int volumen, Map<Zona, List<List<Integer>>> seriesPorZona, CeldaPlantilla celda) {
    }

    public record ResultadoBloque(Bloque bloque, Ejercicio ejercicio, Aproximacion aprox, List<SemanaBloque> semanas) {
    }

    /** {@code dia} = 0 cuando el tipo no tiene ningún ejercicio en el mesociclo (todo el microciclo sin asignar). */
    public record Contador(Tipo tipo, int microciclo, int dia, int planificado, int asignado, int sinAsignar) {
    }

    public record PuntoGrafico(Tipo tipo, int microciclo, int volumen, double imr) {
    }
}
