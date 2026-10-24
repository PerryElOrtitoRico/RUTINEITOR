package com.rutina.service;

import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.PuntoRm;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;
import com.rutina.service.calculo.CalculadoraMesociclo;
import com.rutina.service.calculo.Redondeo;
import com.rutina.service.calculo.ResultadoMesociclo;
import com.rutina.service.calculo.ResultadoMesociclo.PuntoGrafico;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProgresoTest {

    private static final Metodologia M = DatosIniciales.metodologia(); // redondeo de a 2,5 kg

    private static Ejercicio ej(long id, String nombre, Tipo t, Double rm) {
        return new Ejercicio(id, 1, nombre, t, rm, null, 100, Map.of(), null, 0, true);
    }

    private static Mesociclo mes(Character variante, List<Bloque> bloques, Map<Long, Aproximacion> aprox, Map<Long, Double> rms) {
        return new Mesociclo(1, 1, LocalDate.of(2026, 10, 26), 3, new ParametrosTipo(0, 72), new ParametrosTipo(200, 74),
                variante, 3, aprox, bloques, "", "", rms);
    }

    @Test
    void mejorasOrdenadasYSoloConHistorial() {
        var ejs = List.of(ej(1, "Sentadilla", Tipo.FUERZA, 150.0), ej(2, "Press", Tipo.FUERZA, 90.0), ej(3, "Remo", Tipo.FUERZA, 95.0));
        var historial = Map.of(
                1L, List.of(new PuntoRm(LocalDate.of(2026, 9, 1), 140), new PuntoRm(LocalDate.of(2026, 10, 1), 145),
                        new PuntoRm(LocalDate.of(2026, 10, 21), 150)),
                2L, List.of(new PuntoRm(LocalDate.of(2026, 9, 1), 90)),
                3L, List.of(new PuntoRm(LocalDate.of(2026, 9, 1), 100), new PuntoRm(LocalDate.of(2026, 10, 1), 95)));
        var m = Progreso.mejoras(ejs, historial);
        assertEquals(List.of("Sentadilla", "Remo"), m.stream().map(Progreso.Mejora::nombre).toList());
        assertEquals(140, m.get(0).inicial());
        assertEquals(150, m.get(0).actual());
        assertEquals(100.0 * 10 / 140, m.get(0).pct(), 1e-9);
        assertEquals(-5.0, m.get(1).pct(), 1e-9);
    }

    @Test
    void planillaPorMesocicloSumaYPondera() {
        var r1 = new ResultadoMesociclo(List.of(), List.of(), List.of(), List.of(
                new PuntoGrafico(Tipo.FUERZA, 1, 10, 70), new PuntoGrafico(Tipo.FUERZA, 2, 30, 80),
                new PuntoGrafico(Tipo.POTENCIA, 1, 0, 0)), List.of());
        var r2 = new ResultadoMesociclo(List.of(), List.of(), List.of(), List.of(
                new PuntoGrafico(Tipo.POTENCIA, 1, 20, 75)), List.of());
        assertEquals(List.of(new PuntoGrafico(Tipo.FUERZA, 1, 40, 77.5), new PuntoGrafico(Tipo.POTENCIA, 2, 20, 75)),
                Progreso.planillaPorMesociclo(List.of(r1, r2)));
    }

    @Test
    void tonelajeDeFuerzaBasica() throws Exception {
        var ejs = Map.of(1L, ej(1, "Frontal", Tipo.FUERZA, 130.0), 2L, ej(2, "Biceps", Tipo.HIPERTROFIA, null));
        var mes = mes('A', List.of(new Bloque(1, 1, 1L, 100), new Bloque(2, 1, 2L, 100)), Map.of(1L, Aproximacion.BASICA), Map.of());
        var res = CalculadoraMesociclo.calcular(M, mes, ejs);
        // plantilla 3 semanas, 1 vez: 4x6-70% (90 kg) · 4x5-80% (105 kg) · 4x2-95% (122,5 kg); bíceps sin kg no suma
        assertArrayEquals(new double[]{2160, 2100, 980}, Progreso.tonelajePorMicro(mes, res, M), 1e-9);
    }

    @Test
    void tonelajeUsaLosRmDelExport() throws Exception {
        var ejs = Map.of(1L, ej(1, "Frontal", Tipo.FUERZA, 130.0));
        var mes = mes('A', List.of(new Bloque(1, 1, 1L, 100)), Map.of(1L, Aproximacion.BASICA), Map.of(1L, 100.0));
        var res = CalculadoraMesociclo.calcular(M, mes, ejs);
        assertEquals(24 * 70.0, Progreso.tonelajePorMicro(mes, res, M)[0], 1e-9);
    }

    @Test
    void tonelajeDeAproximacionCompleja() throws Exception {
        var ejs = Map.of(1L, ej(1, "Sentadilla", Tipo.FUERZA, 150.0));
        var mes = mes('A', List.of(new Bloque(1, 1, 1L, 100)), Map.of(), Map.of());
        var res = CalculadoraMesociclo.calcular(M, mes, ejs);
        double esperado = 0;
        for (Zona z : Zona.values()) {
            int reps = res.bloques().get(0).semanas().get(0).seriesPorZona().get(z).get(0).stream().mapToInt(Integer::intValue).sum();
            esperado += reps * Redondeo.kg(150, z.pct(), M.incrementoKg());
        }
        assertTrue(esperado > 0);
        assertEquals(esperado, Progreso.tonelajePorMicro(mes, res, M)[0], 1e-9);
    }

    @Test
    void armado() {
        assertTrue(Progreso.armado(mes('A', List.of(new Bloque(1, 1, 1L, 100)), Map.of(), Map.of())));
        assertFalse(Progreso.armado(mes(null, List.of(new Bloque(1, 1, 1L, 100)), Map.of(), Map.of())));
        assertFalse(Progreso.armado(mes('A', List.of(), Map.of(), Map.of())));
    }
}
