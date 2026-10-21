package com.rutina.service.calculo;

import com.rutina.model.CeldaPlantilla;
import com.rutina.model.ClavePlantilla;
import com.rutina.model.Metodologia;
import com.rutina.model.Plantilla;
import com.rutina.model.Tipo;

import java.util.ArrayList;
import java.util.List;

/** Plantillas de fuerza básica e hipertrofia, y su inferencia para 3 o más días por semana (spec §6.4). */
public final class Plantillas {

    private Plantillas() {
    }

    public static Plantilla obtener(Metodologia m, Tipo t, int semanas, int diasPorSemana) {
        Plantilla p = m.plantillas().get(new ClavePlantilla(t, semanas, diasPorSemana));
        if (p == null) {
            throw new IllegalArgumentException("No hay plantilla de " + t.name().toLowerCase() + " para "
                    + semanas + " semanas y " + diasPorSemana + " días por semana.");
        }
        return p;
    }

    /**
     * A partir de la plantilla de 2 días (moderado, pesado) arma la de {@code dias} días:
     * moderado / pesado / liviano, y para 4 y 5 días se repite alternando sin agregar otro día pesado.
     */
    public static Plantilla inferir(Plantilla dosDias, Tipo tipo, int dias) {
        List<List<CeldaPlantilla>> semanas = new ArrayList<>();
        for (List<CeldaPlantilla> semana : dosDias.semanas()) {
            CeldaPlantilla mod = semana.get(0);
            CeldaPlantilla pes = semana.get(1);
            CeldaPlantilla liv = escalonAbajo(mod, tipo);
            List<CeldaPlantilla> patron = List.of(mod, pes, liv, mod, liv);
            semanas.add(List.copyOf(patron.subList(0, dias)));
        }
        return new Plantilla(semanas, true);
    }

    /** Un escalón más liviano: fuerza +1 rep y −5 %; hipertrofia +2 reps. */
    public static CeldaPlantilla escalonAbajo(CeldaPlantilla c, Tipo t) {
        if (t == Tipo.HIPERTROFIA) return new CeldaPlantilla(c.series(), c.reps() + 2, c.pctMin(), c.pctMax());
        return new CeldaPlantilla(c.series(), c.reps() + 1,
                c.pctMin() == null ? null : c.pctMin() - 5,
                c.pctMax() == null ? null : c.pctMax() - 5);
    }
}
