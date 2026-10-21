package com.rutina.service.calculo;

import com.rutina.model.Zona;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reparto de las reps de una zona entre los ejercicios del mismo tipo en un día, respetando el techo. */
public final class RepartoEjercicios {

    private RepartoEjercicios() {
    }

    public static ResultadoReparto repartir(int reps, Zona z, List<Participante> participantes) {
        List<Participante> admiten = participantes.stream()
                .filter(p -> p.techoPct() >= z.pct() && p.pct() > 0)
                .toList();
        Map<Long, Integer> res = new LinkedHashMap<>();
        if (admiten.isEmpty()) return new ResultadoReparto(res, reps);

        double[] pesos = admiten.stream().mapToDouble(Participante::pct).toArray();
        int[] partes = Redondeo.mayorResto(reps, pesos);
        for (int i = 0; i < admiten.size(); i++) {
            if (partes[i] > 0) res.merge(admiten.get(i).ejercicioId(), partes[i], Integer::sum);
        }
        return new ResultadoReparto(res, 0);
    }
}
