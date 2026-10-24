package com.rutina.registro;

import com.google.gson.JsonParser;
import com.rutina.model.Aproximacion;
import com.rutina.model.Tipo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T04. Se corre desde la raíz del repo (mvn test). */
class T04JsonPlanOcultoTest {

    private static final Path EJEMPLO = Path.of("docs/registro-sesiones/ejemplos/plan-ejemplo.json");

    private static PlanCelular chico() {
        var fila = new FilaPlan("80-85", 80, 85, List.of(new SerieReal(3, 120), new SerieReal(3, 127.5)));
        var basica = new FilaPlan("4x12", null, null, List.of(new SerieReal(12, 0)));
        var ej1 = new EjercicioPlan("1-1", "Sentadilla ñandú", Tipo.FUERZA, Aproximacion.COMPLEJA, 150.0, List.of(fila));
        var ej2 = new EjercicioPlan("1-2", "Biceps", Tipo.HIPERTROFIA, Aproximacion.BASICA, null, List.of(basica));
        return new PlanCelular("rutineitor-plan", 1, "Franco", 3, "2026-10-26", 1, 3,
                List.of(new SesionPlan(1, 1, List.of(ej1, ej2)), new SesionPlan(1, 2, List.of()), new SesionPlan(1, 3, List.of())));
    }

    @Test
    void idaYVuelta() {
        assertEquals(chico(), JsonRegistro.planDeJson(JsonRegistro.planAJson(chico())));
    }

    @Test
    void estructuraDelJson() {
        var o = JsonParser.parseString(JsonRegistro.planAJson(chico())).getAsJsonObject();
        assertEquals("rutineitor-plan", o.get("formato").getAsString());
        assertEquals(3, o.get("mesocicloId").getAsLong());
        var ej = o.getAsJsonArray("sesiones").get(0).getAsJsonObject().getAsJsonArray("ejercicios").get(0).getAsJsonObject();
        assertEquals("FUERZA", ej.get("tipo").getAsString());
        assertEquals("COMPLEJA", ej.get("modo").getAsString());
        var serie = ej.getAsJsonArray("filas").get(0).getAsJsonObject().getAsJsonArray("series").get(1).getAsJsonObject();
        assertEquals(3, serie.get("reps").getAsInt());
        assertEquals(127.5, serie.get("kg").getAsDouble());
        var ej2 = o.getAsJsonArray("sesiones").get(0).getAsJsonObject().getAsJsonArray("ejercicios").get(1).getAsJsonObject();
        assertTrue(ej2.has("rm") && ej2.get("rm").isJsonNull(), "rm null tiene que escribirse");
        assertTrue(JsonRegistro.planAJson(chico()).contains("\n"), "con sangría");
    }

    @Test
    void ejemploDeLaEspecificacion() throws Exception {
        var p = JsonRegistro.planDeJson(Files.readString(EJEMPLO));
        assertEquals(4, p.sesiones().size());
        assertEquals(2, p.microciclos());
        var fila = p.sesiones().get(0).ejercicios().get(0).filas().get(1);
        assertEquals("80-85", fila.etiqueta());
        assertEquals(new SerieReal(3, 127.5), fila.series().get(2));
        assertNull(p.sesiones().get(0).ejercicios().get(1).rm());
        assertEquals(Tipo.HIPERTROFIA, p.sesiones().get(0).ejercicios().get(1).tipo());
    }

    @Test
    void escribirEnUtf8(@TempDir Path d) throws Exception {
        var f = d.resolve("plan.json");
        JsonRegistro.escribirPlan(f, chico());
        String texto = Files.readString(f, StandardCharsets.UTF_8);
        assertTrue(texto.contains("ñandú"));
        assertEquals(chico(), JsonRegistro.planDeJson(texto));
    }

    @Test
    void errores() {
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.planDeJson("{\"formato\":\"otro\",\"version\":1,\"sesiones\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.planDeJson("{"));
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.planDeJson("no es json"));
        String v2 = JsonRegistro.planAJson(chico()).replace("\"version\": 1", "\"version\": 2");
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.planDeJson(v2));
        String registro = "{\"formato\":\"rutineitor-registro\",\"version\":1,\"sesiones\":[]}";
        assertThrows(IllegalArgumentException.class, () -> JsonRegistro.planDeJson(registro));
    }
}
