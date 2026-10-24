package com.rutina.service;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.PlanPliometria;
import com.rutina.model.Tipo;
import com.rutina.pdf.PlanConDias;
import com.rutina.service.calculo.Pliometria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Lógica de la pantalla de armado que no es cálculo: solapamientos y valores por defecto. */
public final class Armado {

    private Armado() {
    }

    /** Planes de pliometría con al menos una semana dentro del mesociclo, listos para el PDF. */
    public static List<PlanConDias> planesDelMesociclo(Metodologia m, Mesociclo mes, List<PlanPliometria> planes,
                                                       List<Ejercicio> ejercicios) {
        List<PlanConDias> res = new ArrayList<>();
        for (PlanPliometria p : planes) {
            var semanas = Pliometria.generar(m, p.nivel(), ejercicios);
            Set<Integer> dentro = new TreeSet<>();
            for (int w = 0; w < semanas.size(); w++) {
                LocalDate inicioSemana = p.inicio().plusWeeks(w);
                if (!inicioSemana.isBefore(mes.inicio()) && inicioSemana.isBefore(mes.fin())) dentro.add(w + 1);
            }
            if (!dentro.isEmpty()) res.add(new PlanConDias(p, semanas, dentro));
        }
        return res;
    }

    /** Reparte la bolsa en partes iguales entre los bloques complejos del mismo tipo y día. */
    public static List<Bloque> pctIguales(Mesociclo mes, Map<Long, Ejercicio> ejercicios) {
        List<Bloque> res = new ArrayList<>();
        for (Bloque b : mes.bloques()) {
            Ejercicio e = ejercicios.get(b.ejercicioId());
            if (!esCompleja(e, mes)) {
                res.add(new Bloque(b.dia(), b.posicion(), b.ejercicioId(), 100));
                continue;
            }
            long iguales = mes.bloques().stream()
                    .filter(o -> o.dia() == b.dia())
                    .map(o -> ejercicios.get(o.ejercicioId()))
                    .filter(o -> esCompleja(o, mes) && o.tipo() == e.tipo())
                    .count();
            res.add(new Bloque(b.dia(), b.posicion(), b.ejercicioId(), 100.0 / iguales));
        }
        return res;
    }

    /**
     * Mueve el bloque (dia, posicion) un lugar arriba (delta = -1) o abajo (+1) dentro de su día,
     * intercambiando la posición con el vecino. Si no hay vecino, devuelve la lista igual.
     */
    public static List<Bloque> mover(List<Bloque> bloques, int dia, int posicion, int delta) {
        int destino = posicion + delta;
        boolean hayVecino = bloques.stream().anyMatch(b -> b.dia() == dia && b.posicion() == destino);
        if (!hayVecino) return bloques;
        List<Bloque> res = new ArrayList<>();
        for (Bloque b : bloques) {
            int pos = b.posicion();
            if (b.dia() == dia && pos == posicion) pos = destino;
            else if (b.dia() == dia && pos == destino) pos = posicion;
            res.add(new Bloque(b.dia(), pos, b.ejercicioId(), b.pctBolsa()));
        }
        return res;
    }

    public static boolean esCompleja(Ejercicio e, Mesociclo mes) {
        if (e == null) return false;
        if (e.tipo() == Tipo.POTENCIA) return true;
        return e.tipo() == Tipo.FUERZA
                && mes.aproximaciones().getOrDefault(e.id(), Aproximacion.COMPLEJA) == Aproximacion.COMPLEJA;
    }
}
