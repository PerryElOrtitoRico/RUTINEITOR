package com.rutina.model;

/** Zonas de intensidad del método cubano, de a 5 % del 1RM. */
public enum Zona {
    Z55(55), Z60(60), Z65(65), Z70(70), Z75(75), Z80(80), Z85(85), Z90(90), Z95(95), Z100(100);

    private final int pct;

    Zona(int pct) {
        this.pct = pct;
    }

    public int pct() {
        return pct;
    }

    public static Zona dePct(int pct) {
        for (Zona z : values()) {
            if (z.pct == pct) return z;
        }
        throw new IllegalArgumentException("No existe la zona " + pct + " %");
    }
}
