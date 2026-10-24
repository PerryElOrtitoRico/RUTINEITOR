package com.rutina.service;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.PlanPliometria;
import com.rutina.model.Tipo;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArmadoTest {

    private static Mesociclo mes(List<Bloque> bloques, Map<Long, Aproximacion> aprox) {
        return new Mesociclo(1, 1, LocalDate.of(2026, 11, 2), 3, new ParametrosTipo(250, 72),
                new ParametrosTipo(350, 76), 'A', 3, aprox, bloques, "", "", Map.of());
    }

    private static Ejercicio ej(long id, Tipo t) {
        return new Ejercicio(id, 1, "E" + id, t, 100.0, null, 100, Map.of(), null, 0, true);
    }

    @Test
    void planesQueSeSolapan() {
        var planes = List.of(
                new PlanPliometria(1, 1, 1, LocalDate.of(2026, 10, 12)), // semanas 12/10–2/11: solo la 4.ª cae
                new PlanPliometria(2, 1, 2, LocalDate.of(2026, 11, 16)), // 3.ª semana del mesociclo
                new PlanPliometria(3, 1, 1, LocalDate.of(2026, 9, 1)));  // termina antes
        var res = Armado.planesDelMesociclo(DatosIniciales.metodologia(), mes(List.of(), Map.of()), planes,
                DatosIniciales.ejercicios());
        assertEquals(List.of(1L, 2L), res.stream().map(p -> p.plan().id()).toList());
        assertEquals(Set.of(4), res.get(0).semanasDentroDelMesociclo());
        assertEquals(Set.of(1), res.get(1).semanasDentroDelMesociclo());
        assertEquals(4, res.get(0).semanas().size());
    }

    @Test
    void planQueSeSuperpone() {
        var planes = List.of(new PlanPliometria(1, 1, 2, LocalDate.of(2026, 10, 19)),   // 19/10 al 15/11
                new PlanPliometria(2, 1, 1, LocalDate.of(2026, 12, 21)));
        assertEquals(1L, Armado.planQueSeSuperpone(new PlanPliometria(0, 1, 1, LocalDate.of(2026, 11, 9)), planes).id());
        assertNull(Armado.planQueSeSuperpone(new PlanPliometria(0, 1, 1, LocalDate.of(2026, 11, 16)), planes)); // empieza justo al terminar
        assertNull(Armado.planQueSeSuperpone(new PlanPliometria(1, 1, 3, LocalDate.of(2026, 10, 26)), planes)); // editarse a sí mismo
        assertEquals(2L, Armado.planQueSeSuperpone(new PlanPliometria(1, 1, 3, LocalDate.of(2026, 12, 7)), planes).id());
    }

    @Test
    void moverIntercambiaConElVecinoDelMismoDia() {
        var bloques = List.of(new Bloque(1, 1, 10L, 100), new Bloque(1, 2, 20L, 100), new Bloque(2, 1, 30L, 100));
        var res = Armado.mover(bloques, 1, 2, -1);
        assertEquals(List.of(new Bloque(1, 2, 10L, 100), new Bloque(1, 1, 20L, 100), new Bloque(2, 1, 30L, 100)), res);
    }

    @Test
    void moverFueraDelDiaNoCambiaNada() {
        var bloques = List.of(new Bloque(1, 1, 10L, 100), new Bloque(1, 2, 20L, 100));
        assertEquals(bloques, Armado.mover(bloques, 1, 1, -1));
        assertEquals(bloques, Armado.mover(bloques, 1, 2, +1));
    }

    @Test
    void pctIgualesEntreComplejosDelMismoTipoYDia() {
        var bloques = List.of(new Bloque(1, 1, 1L, 100), new Bloque(1, 2, 2L, 100), new Bloque(1, 3, 3L, 100),
                new Bloque(2, 1, 1L, 100), new Bloque(1, 4, 4L, 100));
        var ejs = Map.of(1L, ej(1, Tipo.FUERZA), 2L, ej(2, Tipo.FUERZA), 3L, ej(3, Tipo.FUERZA), 4L, ej(4, Tipo.POTENCIA));
        var res = Armado.pctIguales(mes(bloques, Map.of(3L, Aproximacion.BASICA)), ejs);
        assertEquals(List.of(50.0, 50.0, 100.0, 100.0, 100.0), res.stream().map(Bloque::pctBolsa).toList());
    }
}
