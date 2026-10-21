package com.rutina.model;

/** Una variante de mesociclo (por ejemplo 4A): % del volumen y variación de IMR de cada microciclo. */
public record VarianteMicro(double[] volPct, int[] deltaImr) {
}
