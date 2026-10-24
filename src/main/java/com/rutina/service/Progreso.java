package com.rutina.service;

import com.rutina.model.CeldaPlantilla;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.PuntoRm;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;
import com.rutina.service.calculo.Redondeo;
import com.rutina.service.calculo.ResultadoMesociclo;
import com.rutina.service.calculo.ResultadoMesociclo.PuntoGrafico;
import com.rutina.service.calculo.ResultadoMesociclo.ResultadoBloque;
import com.rutina.service.calculo.ResultadoMesociclo.SemanaBloque;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Cálculos de la pestaña Progreso: lo que muestra la planilla a lo largo de los mesociclos. */
public final class Progreso {

    /** Cambio del 1RM de un ejercicio entre su primer y su último valor del historial. */
    public record Mejora(String nombre, double inicial, double actual, double pct) {
    }

    private Progreso() {
    }

    /** Mesociclo con variante, días y al menos un ejercicio: se puede calcular. */
    public static boolean armado(Mesociclo m) {
        return m.variante() != null && m.dias() != null && !m.bloques().isEmpty();
    }

    /** Ejercicios con 2 o más valores de 1RM, de mayor a menor mejora. */
    public static List<Mejora> mejoras(List<Ejercicio> ejercicios, Map<Long, List<PuntoRm>> historial) {
        List<Mejora> res = new ArrayList<>();
        for (Ejercicio e : ejercicios) {
            List<PuntoRm> h = historial.getOrDefault(e.id(), List.of());
            if (h.size() < 2 || h.get(0).rm() <= 0) continue;
            double inicial = h.get(0).rm();
            double actual = h.get(h.size() - 1).rm();
            res.add(new Mejora(e.nombre(), inicial, actual, (actual - inicial) * 100 / inicial));
        }
        res.sort(Comparator.comparingDouble(Mejora::pct).reversed());
        return res;
    }

    /**
     * Un punto por tipo y mesociclo con el volumen de la planilla (suma de sus microciclos) y su IMR ponderado.
     * El campo "microciclo" del punto es el número de mesociclo (1, 2…) en el orden recibido.
     */
    public static List<PuntoGrafico> planillaPorMesociclo(List<ResultadoMesociclo> resultados) {
        List<PuntoGrafico> res = new ArrayList<>();
        for (int i = 0; i < resultados.size(); i++) {
            for (Tipo t : Tipo.values()) {
                int volumen = 0;
                double suma = 0;
                for (PuntoGrafico p : resultados.get(i).real()) {
                    if (p.tipo() != t) continue;
                    volumen += p.volumen();
                    suma += p.volumen() * p.imr();
                }
                if (volumen > 0) res.add(new PuntoGrafico(t, i + 1, volumen, suma / volumen));
            }
        }
        return res;
    }

    /**
     * Tonelaje (reps × kg) de la planilla en cada microciclo. Solo cuentan los ejercicios con 1RM
     * (el usado al exportar o, si no hay, el actual); hipertrofia no tiene kg y no suma.
     */
    public static double[] tonelajePorMicro(Mesociclo mes, ResultadoMesociclo res, Metodologia m) {
        double[] t = new double[mes.microciclos()];
        for (ResultadoBloque rb : res.bloques()) {
            Double rm = mes.rmUsados().getOrDefault(rb.ejercicio().id(), rb.ejercicio().rm());
            if (rm == null || rb.ejercicio().tipo() == Tipo.HIPERTROFIA) continue;
            for (int w = 0; w < rb.semanas().size() && w < t.length; w++) {
                SemanaBloque s = rb.semanas().get(w);
                CeldaPlantilla c = s.celda();
                if (c != null) {
                    if (c.pctMin() != null) {
                        t[w] += c.series() * c.reps() * Redondeo.kg(rm, (c.pctMin() + c.pctMax()) / 2.0, m.incrementoKg());
                    }
                    continue;
                }
                for (Map.Entry<Zona, List<List<Integer>>> en : s.seriesPorZona().entrySet()) {
                    int reps = en.getValue().isEmpty() ? 0 : en.getValue().get(0).stream().mapToInt(Integer::intValue).sum();
                    t[w] += reps * Redondeo.kg(rm, en.getKey().pct(), m.incrementoKg());
                }
            }
        }
        return t;
    }
}
