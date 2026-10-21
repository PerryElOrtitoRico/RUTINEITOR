package com.rutina.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Una celda de plantilla básica o de hipertrofia: "4x12", "3x3-90%" o "3x5-75/80%".
 * Sin porcentaje, {@code pctMin} y {@code pctMax} son null.
 */
public record CeldaPlantilla(int series, int reps, Integer pctMin, Integer pctMax) {

    private static final Pattern FORMATO =
            Pattern.compile("\\s*(\\d+)\\s*x\\s*(\\d+)\\s*(?:-\\s*(\\d+)(?:\\s*/\\s*(\\d+))?\\s*%)?\\s*");

    public static CeldaPlantilla parse(String texto) {
        Matcher m = FORMATO.matcher(texto == null ? "" : texto);
        if (!m.matches()) {
            throw new IllegalArgumentException("Formato de celda inválido: \"" + texto + "\" (ejemplos: 4x12, 3x3-90%, 3x5-75/80%)");
        }
        int series = Integer.parseInt(m.group(1));
        int reps = Integer.parseInt(m.group(2));
        if (m.group(3) == null) return new CeldaPlantilla(series, reps, null, null);
        int min = Integer.parseInt(m.group(3));
        int max = m.group(4) == null ? min : Integer.parseInt(m.group(4));
        return new CeldaPlantilla(series, reps, min, max);
    }

    public String texto() {
        String base = series + "x" + reps;
        if (pctMin == null) return base;
        if (pctMin.equals(pctMax)) return base + "-" + pctMin + "%";
        return base + "-" + pctMin + "/" + pctMax + "%";
    }
}
