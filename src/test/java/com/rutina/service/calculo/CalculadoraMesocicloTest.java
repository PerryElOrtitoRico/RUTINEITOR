package com.rutina.service.calculo;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;
import com.rutina.service.calculo.ResultadoMesociclo.Contador;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraMesocicloTest {

    private static final Metodologia M = DatosIniciales.metodologia();

    private static Ejercicio fuerza(long id, Double rm, int techo) {
        return new Ejercicio(id, 1, "Sentadilla " + id, Tipo.FUERZA, rm, null, techo, Map.of(), null, 0, true);
    }

    /** 2A, 3 días, fuerza 200 @ 70; un bloque complejo el día 1. */
    private static Mesociclo mesociclo(ParametrosTipo potencia, Character variante, List<Bloque> bloques) {
        return new Mesociclo(0, 1, LocalDate.of(2026, 11, 2), 2, potencia, new ParametrosTipo(200, 70),
                variante, 3, Map.of(1L, Aproximacion.COMPLEJA, 2L, Aproximacion.COMPLEJA), bloques, "", "", Map.of());
    }

    private static ResultadoMesociclo calc() throws ParametrosInvalidosException {
        return CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(0, 72), 'A',
                List.of(new Bloque(1, 1, 1L, 100))), Map.of(1L, fuerza(1, 100.0, 100)));
    }

    @Test
    void microciclo1() throws Exception {
        var r = calc();
        var sem = r.bloques().get(0).semanas().get(0);
        assertEquals(List.of(List.of(3, 3, 3, 3, 3, 3)), sem.seriesPorZona().get(Zona.Z80));
        assertEquals(List.of(List.of(4, 4)), sem.seriesPorZona().get(Zona.Z55));
        assertEquals(86, sem.volumen());
        assertEquals(80, r.planificado().stream().filter(p -> p.tipo() == Tipo.FUERZA && p.microciclo() == 1)
                .findFirst().get().volumen());
        assertEquals(86, r.real().stream().filter(p -> p.tipo() == Tipo.FUERZA && p.microciclo() == 1)
                .findFirst().get().volumen());
    }

    @Test
    void contadorDiaUnico() throws Exception {
        var c = calc().contadores().stream()
                .filter(x -> x.tipo() == Tipo.FUERZA && x.microciclo() == 1 && x.dia() == 1).findFirst().get();
        assertEquals(80, c.planificado());
        assertEquals(86, c.asignado());
        assertEquals(0, c.sinAsignar());
    }

    @Test
    void tipoSinEjercicios() throws Exception {
        var r = CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(100, 72), 'A',
                List.of(new Bloque(1, 1, 1L, 100))), Map.of(1L, fuerza(1, 100.0, 100)));
        assertEquals(40, r.contadores().stream().filter(x -> x.tipo() == Tipo.POTENCIA && x.microciclo() == 1)
                .mapToInt(Contador::sinAsignar).sum());
    }

    @Test
    void faltaVariante() {
        assertThrows(ParametrosInvalidosException.class, () -> CalculadoraMesociclo.calcular(M,
                mesociclo(new ParametrosTipo(0, 72), null, List.of()), Map.of()));
    }

    @Test
    void ejercicioSinRmNoRompe() throws Exception {
        var r = CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(0, 72), 'A',
                List.of(new Bloque(1, 1, 1L, 100))), Map.of(1L, fuerza(1, null, 100)));
        assertEquals(2, r.bloques().get(0).semanas().size());
    }

    @Test
    void avisoPctBolsa() throws Exception {
        var r = CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(0, 72), 'A',
                        List.of(new Bloque(1, 1, 1L, 60), new Bloque(1, 2, 2L, 30))),
                Map.of(1L, fuerza(1, 100.0, 100), 2L, fuerza(2, 100.0, 100)));
        assertFalse(r.avisos().isEmpty());
    }

    @Test
    void techoMandaLasZonasAltasAlOtro() throws Exception {
        var r = CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(0, 72), 'A',
                        List.of(new Bloque(1, 1, 1L, 50), new Bloque(1, 2, 2L, 50))),
                Map.of(1L, fuerza(1, 100.0, 85), 2L, fuerza(2, 100.0, 100)));
        var conTecho = r.bloques().get(0).semanas().get(0).seriesPorZona();
        var sinTecho = r.bloques().get(1).semanas().get(0).seriesPorZona();
        assertTrue(conTecho.get(Zona.Z90).get(0).isEmpty());
        assertEquals(List.of(List.of(2, 2, 2)), sinTecho.get(Zona.Z90)); // 5 reps de 90 % → 3 series de 2
    }

    @Test
    void hipertrofiaUsaPlantilla() throws Exception {
        var hip = new Ejercicio(3, 1, "Biceps", Tipo.HIPERTROFIA, null, null, 100, Map.of(), null, 0, true);
        var r = CalculadoraMesociclo.calcular(M, mesociclo(new ParametrosTipo(0, 72), 'A',
                List.of(new Bloque(2, 1, 3L, 100))), Map.of(3L, hip));
        assertEquals("4x12", r.bloques().get(0).semanas().get(0).celda().texto());
        assertEquals("4x10", r.bloques().get(0).semanas().get(1).celda().texto());
    }
}
