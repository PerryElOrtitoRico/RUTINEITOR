package com.rutina.registro;

import com.rutina.model.Aproximacion;
import com.rutina.model.Tipo;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T07. */
class T07AnalisisOcultoTest {

    private static final double EPS = 1e-9;

    private static PlanCelular planEjemplo() throws Exception {
        return JsonRegistro.planDeJson(Files.readString(Path.of("docs/registro-sesiones/ejemplos/plan-ejemplo.json")));
    }

    private static RegistroCelular reg(SesionRegistro... s) {
        return new RegistroCelular("rutineitor-registro", 1, "Franco", 3, List.of(s));
    }

    private static ComparacionTipo tipo(ResumenSesion r, Tipo t) {
        return r.porTipo().stream().filter(c -> c.tipo() == t).findFirst().orElseThrow();
    }

    @Test
    void ejemplosDeLaTarea() throws Exception {
        var res = AnalisisRegistro.analizar(planEjemplo(), JsonRegistro.leerRegistro(Path.of("docs/registro-sesiones/ejemplos/registro-ejemplo.json")));
        assertEquals(2, res.size());
        var s1 = res.get(0);
        assertEquals("2026-10-27", s1.fecha());
        assertEquals(4.0, s1.esfuerzoPromedio());
        assertEquals(List.of(Tipo.FUERZA, Tipo.HIPERTROFIA), s1.porTipo().stream().map(ComparacionTipo::tipo).toList());
        var f = tipo(s1, Tipo.FUERZA);
        assertEquals(17, f.volumenPlan());
        assertEquals(14, f.volumenReal());
        assertEquals(1295.0 / 17, f.imrPlan(), EPS);
        assertEquals(1030.0 / 14, f.imrReal(), EPS);
        var h = tipo(s1, Tipo.HIPERTROFIA);
        assertEquals(48, h.volumenReal());
        assertNull(h.imrPlan());
        assertNull(h.imrReal());
        var s2 = res.get(1);
        assertNull(s2.esfuerzoPromedio());
        assertNull(s2.sensacionFinal());
        assertEquals(90.0 / 130 * 100, tipo(s2, Tipo.FUERZA).imrReal(), EPS);
    }

    @Test
    void ordenYSesionesDesconocidas() throws Exception {
        var res = AnalisisRegistro.analizar(planEjemplo(), reg(
                new SesionRegistro(2, 2, "2026-11-05", null, null, List.of()),
                new SesionRegistro(9, 9, "2026-11-06", null, null, List.of()),   // no existe en el plan
                new SesionRegistro(1, 2, "2026-10-29", null, null, List.of())));
        assertEquals(List.of("1-2", "2-2"), res.stream().map(r -> r.microciclo() + "-" + r.dia()).toList());
    }

    @Test
    void ceroYFilaVaciaYEspacios() throws Exception {
        var res = AnalisisRegistro.analizar(planEjemplo(), reg(new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(
                new EjercicioRegistro("1-1", 2, Map.of("70", "0", "80-85", "   ")),
                new EjercicioRegistro("1-2", 5, Map.of("4x12", "12.12-10")))))).get(0);
        var f = tipo(res, Tipo.FUERZA);
        assertEquals(9, f.volumenReal());           // la fila 70 no se hizo; la 80-85 en blanco = planificada
        assertEquals((240 + 240 + 255) / 9.0, f.imrReal(), EPS);
        var h = tipo(res, Tipo.HIPERTROFIA);
        assertEquals(24, h.volumenReal());
        assertNull(h.imrReal());                      // sin rm ni %: el kg anotado no alcanza
        assertEquals(3.5, res.esfuerzoPromedio(), EPS);
    }

    @Test
    void ningunaSerieRealDaImrNull() throws Exception {
        var res = AnalisisRegistro.analizar(planEjemplo(), reg(new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(
                new EjercicioRegistro("1-1", null, Map.of("70", "0", "80-85", "0")))))).get(0);
        assertEquals(0, tipo(res, Tipo.FUERZA).volumenReal());
        assertNull(tipo(res, Tipo.FUERZA).imrReal());
        assertNotNull(tipo(res, Tipo.FUERZA).imrPlan());
    }

    @Test
    void sinRmUsaElPorcentajeDeLaFila() {
        var fila = new FilaPlan("90-95", 90, 95, List.of(new SerieReal(2, 0), new SerieReal(1, 0)));
        var plan = new PlanCelular("rutineitor-plan", 1, "F", 3, "2026-10-26", 1, 1, List.of(new SesionPlan(1, 1, List.of(
                new EjercicioPlan("1-1", "Cargada", Tipo.POTENCIA, Aproximacion.COMPLEJA, null, List.of(fila))))));
        var r = AnalisisRegistro.analizar(plan, reg(new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(
                new EjercicioRegistro("1-1", null, Map.of("90-95", "2-100.2")))))).get(0);
        var c = tipo(r, Tipo.POTENCIA);
        assertEquals(3, c.volumenPlan());
        assertEquals(4, c.volumenReal());
        assertEquals(92.5, c.imrPlan(), EPS);
        assertEquals(92.5, c.imrReal(), EPS);
    }

    @Test
    void errores() throws Exception {
        var otro = new RegistroCelular("rutineitor-registro", 1, "F", 99, List.of());
        assertThrows(IllegalArgumentException.class, () -> AnalisisRegistro.analizar(planEjemplo(), otro));
        var e = assertThrows(IllegalArgumentException.class, () -> AnalisisRegistro.analizar(planEjemplo(),
                reg(new SesionRegistro(1, 1, "2026-10-27", null, null, List.of(new EjercicioRegistro("1-1", null, Map.of("80-85", "3..3")))))));
        assertTrue(e.getMessage().contains("1-1") && e.getMessage().contains("80-85"), e.getMessage());
    }
}
