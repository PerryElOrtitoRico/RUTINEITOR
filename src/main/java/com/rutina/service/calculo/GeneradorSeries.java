package com.rutina.service.calculo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Convierte las reps de una zona en series de tamaño fijo, arrastrando el resto al día siguiente (spec §6.3). */
public final class GeneradorSeries {

    private GeneradorSeries() {
    }

    public static SeriesZona generar(int[] repsPorDia, int repsPorSerie) {
        List<List<Integer>> porDia = new ArrayList<>();
        int arrastre = 0;
        int planificado = 0;
        int enSeries = 0;
        for (int reps : repsPorDia) {
            planificado += reps;
            int r = reps + arrastre;
            if (r <= 0) {
                porDia.add(List.of());
                arrastre = r;
                continue;
            }
            int n = Redondeo.halfUp((double) r / repsPorSerie);
            if (n == 0) {
                porDia.add(List.of(r));
                enSeries += r;
                arrastre = 0;
            } else {
                porDia.add(Collections.nCopies(n, repsPorSerie));
                enSeries += n * repsPorSerie;
                arrastre = r - n * repsPorSerie;
            }
        }
        return new SeriesZona(porDia, enSeries - planificado);
    }

    /** Notación del libro: [3, 3, 3] → "3.3.3". */
    public static String notacion(List<Integer> series) {
        return series.stream().map(String::valueOf).collect(Collectors.joining("."));
    }
}
