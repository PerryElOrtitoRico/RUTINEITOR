package com.rutina.service.calculo;

import com.rutina.model.Ejercicio;
import com.rutina.model.Metodologia;
import com.rutina.model.Tipo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Plan de pliometría: 6 ejercicios por día, series de 10 saltos (spec §6.5). */
public final class Pliometria {

    public static final int EJERCICIOS_POR_DIA = 6;
    public static final int SALTOS_POR_SERIE = 10;

    private Pliometria() {
    }

    /** Resultado indexado [semana 0..3][día]. */
    public static List<List<DiaPlio>> generar(Metodologia m, int nivel, List<Ejercicio> ejerciciosPlio) {
        int[][] vol = m.volPlio().get(nivel);
        if (vol == null) throw new IllegalArgumentException("No existe el nivel de pliometría " + nivel);
        List<List<DiaPlio>> semanas = new ArrayList<>();
        for (int[] semana : vol) {
            List<DiaPlio> dias = new ArrayList<>();
            for (int d = 0; d < semana.length; d++) {
                String grupo = nivel + String.valueOf((char) ('A' + d));
                int series = Redondeo.halfUp(semana[d] / (double) (EJERCICIOS_POR_DIA * SALTOS_POR_SERIE));
                dias.add(new DiaPlio(semana[d], seisDelGrupo(ejerciciosPlio, grupo), series));
            }
            semanas.add(dias);
        }
        return semanas;
    }

    private static List<String> seisDelGrupo(List<Ejercicio> ejercicios, String grupo) {
        List<String> lista = ejercicios.stream()
                .filter(e -> e.tipo() == Tipo.PLIOMETRIA && e.activo() && grupo.equals(e.grupoPlio()))
                .sorted(Comparator.comparingInt(Ejercicio::orden))
                .map(Ejercicio::nombre)
                .toList();
        if (lista.isEmpty()) return List.of();
        List<String> res = new ArrayList<>();
        for (int i = 0; i < EJERCICIOS_POR_DIA; i++) res.add(lista.get(i % lista.size()));
        return res;
    }
}
