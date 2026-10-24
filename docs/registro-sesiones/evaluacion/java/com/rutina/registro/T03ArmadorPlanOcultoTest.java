package com.rutina.registro;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;
import com.rutina.service.calculo.CalculadoraMesociclo;
import com.rutina.service.calculo.ResultadoMesociclo;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T03. */
class T03ArmadorPlanOcultoTest {

    private static final Metodologia M = DatosIniciales.metodologia();
    private static final Ejercicio SENTADILLA = new Ejercicio(1, 1, "Sentadilla", Tipo.FUERZA, 150.0, null, 100, Map.of(), null, 0, true);
    private static final Ejercicio CARGADA = new Ejercicio(2, 1, "Cargada", Tipo.POTENCIA, null, null, 100, Map.of(), null, 0, true);
    private static final Ejercicio FRONTAL = new Ejercicio(3, 1, "Frontal", Tipo.FUERZA, 130.0, null, 100, Map.of(), null, 0, true);
    private static final Ejercicio BICEPS = new Ejercicio(4, 1, "Biceps", Tipo.HIPERTROFIA, null, null, 100, Map.of(), null, 0, true);

    private static Mesociclo mes(Map<Long, Double> rmUsados) {
        return new Mesociclo(7, 1, LocalDate.of(2026, 10, 26), 3, new ParametrosTipo(150, 72), new ParametrosTipo(250, 74),
                'A', 3, Map.of(1L, Aproximacion.COMPLEJA, 3L, Aproximacion.BASICA),
                List.of(new Bloque(1, 2, 1L, 100), new Bloque(1, 1, 2L, 100), new Bloque(2, 1, 3L, 100), new Bloque(3, 1, 4L, 100)),
                "", "", rmUsados);
    }

    private static final Map<Long, Ejercicio> EJS = Map.of(1L, SENTADILLA, 2L, CARGADA, 3L, FRONTAL, 4L, BICEPS);

    private static SesionPlan sesion(PlanCelular p, int micro, int dia) {
        return p.sesiones().stream().filter(s -> s.microciclo() == micro && s.dia() == dia).findFirst().orElseThrow();
    }

    @Test
    void cabeceraYOrdenDeSesiones() throws Exception {
        var mes = mes(Map.of());
        var p = ArmadorPlan.armar("Franco", mes, CalculadoraMesociclo.calcular(M, mes, EJS), M);
        assertEquals("rutineitor-plan", p.formato());
        assertEquals(1, p.version());
        assertEquals("Franco", p.perfil());
        assertEquals(7, p.mesocicloId());
        assertEquals("2026-10-26", p.inicio());
        assertEquals(3, p.microciclos());
        assertEquals(3, p.dias());
        assertEquals(9, p.sesiones().size());
        int k = 0;
        for (int micro = 1; micro <= 3; micro++) {
            for (int dia = 1; dia <= 3; dia++) {
                assertEquals(micro, p.sesiones().get(k).microciclo());
                assertEquals(dia, p.sesiones().get(k).dia());
                k++;
            }
        }
    }

    @Test
    void ejerciciosOrdenadosPorPosicion() throws Exception {
        var mes = mes(Map.of());
        var p = ArmadorPlan.armar("Franco", mes, CalculadoraMesociclo.calcular(M, mes, EJS), M);
        var s = sesion(p, 2, 1);
        assertEquals(List.of("1-1", "1-2"), s.ejercicios().stream().map(EjercicioPlan::bloque).toList());
        assertEquals("Cargada", s.ejercicios().get(0).nombre());
        assertEquals(Tipo.POTENCIA, s.ejercicios().get(0).tipo());
        assertEquals(Aproximacion.COMPLEJA, s.ejercicios().get(0).modo());
    }

    @Test
    void filasComplejasCoincidenConElCalculo() throws Exception {
        var mes = mes(Map.of());
        ResultadoMesociclo res = CalculadoraMesociclo.calcular(M, mes, EJS);
        var p = ArmadorPlan.armar("Franco", mes, res, M);
        var rb = res.bloques().stream().filter(b -> b.ejercicio().id() == 1).findFirst().orElseThrow();
        String[][] grupos = {{"55"}, {"60"}, {"65"}, {"70"}, {"75"}, {"80-85"}, {"90-95"}, {"100"}};
        Zona[][] zonas = {{Zona.Z55}, {Zona.Z60}, {Zona.Z65}, {Zona.Z70}, {Zona.Z75}, {Zona.Z80, Zona.Z85},
                {Zona.Z90, Zona.Z95}, {Zona.Z100}};
        for (int micro = 1; micro <= 3; micro++) {
            var semana = rb.semanas().get(micro - 1);
            var ej = sesion(p, micro, 1).ejercicios().stream().filter(e -> e.bloque().equals("1-2")).findFirst().orElseThrow();
            assertEquals(150.0, ej.rm());
            List<FilaPlan> esperadas = new ArrayList<>();
            for (int g = 0; g < grupos.length; g++) {
                List<SerieReal> series = new ArrayList<>();
                for (Zona z : zonas[g]) {
                    double kg = com.rutina.service.calculo.Redondeo.kg(150.0, z.pct(), M.incrementoKg());
                    for (int reps : semana.seriesPorZona().get(z).get(0)) series.add(new SerieReal(reps, kg));
                }
                if (!series.isEmpty()) {
                    esperadas.add(new FilaPlan(grupos[g][0], zonas[g][0].pct(), zonas[g][zonas[g].length - 1].pct(), series));
                }
            }
            assertEquals(esperadas, ej.filas(), "microciclo " + micro);
        }
    }

    @Test
    void sinRmKgCero() throws Exception {
        var mes = mes(Map.of());
        var p = ArmadorPlan.armar("Franco", mes, CalculadoraMesociclo.calcular(M, mes, EJS), M);
        var cargada = sesion(p, 1, 1).ejercicios().get(0);
        assertNull(cargada.rm());
        assertFalse(cargada.filas().isEmpty());
        assertTrue(cargada.filas().stream().flatMap(f -> f.series().stream()).allMatch(s -> s.kg() == 0.0));
    }

    @Test
    void rmUsadosTienePrioridad() throws Exception {
        var mes = mes(Map.of(1L, 100.0));
        var p = ArmadorPlan.armar("Franco", mes, CalculadoraMesociclo.calcular(M, mes, EJS), M);
        var ej = sesion(p, 1, 1).ejercicios().get(1);
        assertEquals(100.0, ej.rm());
        var fila80 = ej.filas().stream().filter(f -> f.etiqueta().equals("80-85")).findFirst().orElseThrow();
        assertTrue(fila80.series().stream().allMatch(s -> s.kg() == 80.0 || s.kg() == 85.0));
    }

    @Test
    void basicaEHipertrofia() throws Exception {
        var mes = mes(Map.of());
        ResultadoMesociclo res = CalculadoraMesociclo.calcular(M, mes, EJS);
        var p = ArmadorPlan.armar("Franco", mes, res, M);
        for (int micro = 1; micro <= 3; micro++) {
            var frontal = sesion(p, micro, 2).ejercicios().get(0);
            assertEquals(Aproximacion.BASICA, frontal.modo());
            var celda = res.bloques().stream().filter(b -> b.ejercicio().id() == 3).findFirst().orElseThrow()
                    .semanas().get(micro - 1).celda();
            assertEquals(1, frontal.filas().size());
            var fila = frontal.filas().get(0);
            assertEquals(celda.texto(), fila.etiqueta());
            assertEquals(celda.pctMin(), fila.pctMin());
            assertEquals(celda.pctMax(), fila.pctMax());
            double kg = com.rutina.service.calculo.Redondeo.kg(130.0, (celda.pctMin() + celda.pctMax()) / 2.0, M.incrementoKg());
            assertEquals(java.util.Collections.nCopies(celda.series(), new SerieReal(celda.reps(), kg)), fila.series());

            var biceps = sesion(p, micro, 3).ejercicios().get(0);
            assertEquals(Tipo.HIPERTROFIA, biceps.tipo());
            assertEquals(Aproximacion.BASICA, biceps.modo());
            assertNull(biceps.filas().get(0).pctMin());
            assertTrue(biceps.filas().get(0).series().stream().allMatch(s -> s.kg() == 0.0));
        }
        assertEquals("4x12", sesion(p, 1, 3).ejercicios().get(0).filas().get(0).etiqueta());
    }

    @Test
    void listasInmutables() throws Exception {
        var mes = mes(Map.of());
        var p = ArmadorPlan.armar("Franco", mes, CalculadoraMesociclo.calcular(M, mes, EJS), M);
        assertThrows(UnsupportedOperationException.class, () -> p.sesiones().clear());
        assertThrows(UnsupportedOperationException.class, () -> p.sesiones().get(0).ejercicios().clear());
    }
}
