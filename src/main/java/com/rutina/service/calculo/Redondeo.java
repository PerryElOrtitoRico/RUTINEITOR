package com.rutina.service.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Reglas de redondeo comunes a todo el cálculo (siempre HALF_UP). */
public final class Redondeo {

    private static final double EPS = 1e-9;

    private Redondeo() {
    }

    public static int halfUp(double x) {
        return new BigDecimal(Double.toString(x)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    /**
     * Reparte {@code total} en partes enteras proporcionales a {@code pesos} por el método del mayor resto.
     * La suma del resultado es exactamente {@code total}; a igual fracción gana el índice menor.
     */
    public static int[] mayorResto(int total, double[] pesos) {
        int[] res = new int[pesos.length];
        double suma = 0;
        for (double p : pesos) suma += p;
        if (suma <= 0 || total == 0) return res;

        double[] fraccion = new double[pesos.length];
        int asignado = 0;
        for (int i = 0; i < pesos.length; i++) {
            double exacto = total * pesos[i] / suma;
            res[i] = (int) Math.floor(exacto + EPS);
            fraccion[i] = exacto - res[i];
            asignado += res[i];
        }
        List<Integer> orden = new ArrayList<>();
        for (int i = 0; i < pesos.length; i++) {
            if (pesos[i] > 0) orden.add(i);
        }
        orden.sort((a, b) -> {
            if (Math.abs(fraccion[a] - fraccion[b]) > EPS) return Double.compare(fraccion[b], fraccion[a]);
            return Integer.compare(a, b);
        });
        for (int k = 0; asignado < total; k++, asignado++) {
            res[orden.get(k % orden.size())]++;
        }
        return res;
    }

    /** kg = rm × pct / 100, redondeado al múltiplo de {@code incremento} más cercano. */
    public static double kg(double rm, double pct, double incremento) {
        BigDecimal valor = BigDecimal.valueOf(rm).multiply(BigDecimal.valueOf(pct)).divide(BigDecimal.valueOf(100));
        BigDecimal paso = BigDecimal.valueOf(incremento);
        BigDecimal pasos = valor.divide(paso, 0, RoundingMode.HALF_UP);
        return pasos.multiply(paso).doubleValue();
    }
}
