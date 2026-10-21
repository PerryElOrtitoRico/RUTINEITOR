package com.rutina.service.calculo;

import com.rutina.model.Metodologia;

/** Repeticiones por zona de intensidad (método cubano, spec §6.2). */
public final class DistribucionZonas {

    private DistribucionZonas() {
    }

    /** Reps del microciclo en cada una de las 10 zonas; la fila de la tabla se normaliza al repartir. */
    public static int[] repsPorZona(Metodologia m, int volumen, int imr) {
        return Redondeo.mayorResto(volumen, m.ref().get(imr));
    }

    public static int[] repartirEnDias(int reps, double[] pesosDias) {
        return Redondeo.mayorResto(reps, pesosDias);
    }
}
