package com.rutina.service.calculo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convierte repeticiones en series de tamaño fijo, arrastrando el resto al paso siguiente (spec §6.3).
 * La calculadora lo usa sobre las zonas de un día, de la más liviana a la más pesada.
 */
public final class GeneradorSeries {

    private GeneradorSeries() {
    }

    public static SeriesZona generar(int[] reps, int repsPorSerie) {
        int[] s = new int[reps.length];
        Arrays.fill(s, repsPorSerie);
        return generar(reps, s);
    }

    /** {@code repsPorSerie[i]} es el tamaño de serie del paso i. */
    public static SeriesZona generar(int[] reps, int[] repsPorSerie) {
        List<List<Integer>> porPaso = new ArrayList<>();
        int arrastre = 0;
        int planificado = 0;
        int enSeries = 0;
        for (int i = 0; i < reps.length; i++) {
            planificado += reps[i];
            int r = reps[i] + arrastre;
            if (r <= 0) {
                porPaso.add(List.of());
                arrastre = r;
                continue;
            }
            // Nunca series sueltas de otro tamaño: si no alcanza para media serie, pasa al paso siguiente
            int s = repsPorSerie[i];
            int n = Redondeo.halfUp((double) r / s);
            porPaso.add(n == 0 ? List.of() : Collections.nCopies(n, s));
            enSeries += n * s;
            arrastre = r - n * s;
        }
        return new SeriesZona(porPaso, enSeries - planificado);
    }

    /** Notación del libro: [3, 3, 3] → "3.3.3". */
    public static String notacion(List<Integer> series) {
        return series.stream().map(String::valueOf).collect(Collectors.joining("."));
    }
}
