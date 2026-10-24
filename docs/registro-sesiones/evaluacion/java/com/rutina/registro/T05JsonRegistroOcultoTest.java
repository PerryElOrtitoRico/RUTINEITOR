package com.rutina.registro;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T05. */
class T05JsonRegistroOcultoTest {

    private static final Path EJEMPLO = Path.of("docs/registro-sesiones/ejemplos/registro-ejemplo.json");

    private static String con(String sesiones) {
        return "{\"formato\":\"rutineitor-registro\",\"version\":1,\"perfil\":\"F\",\"mesocicloId\":3,\"sesiones\":" + sesiones + "}";
    }

    @Test
    void ejemplo() throws Exception {
        var r = JsonRegistro.leerRegistro(EJEMPLO);
        assertEquals("rutineitor-registro", r.formato());
        assertEquals(3, r.mesocicloId());
        assertEquals(2, r.sesiones().size());
        var s1 = r.sesiones().get(0);
        assertEquals(1, s1.microciclo());
        assertEquals(1, s1.dia());
        assertEquals("2026-10-27", s1.fecha());
        assertEquals(5, s1.descanso());
        assertEquals(4, s1.sensacionFinal());
        assertEquals(new EjercicioRegistro("1-1", 4, Map.of("80-85", "3.3-115")), s1.ejercicios().get(0));
        assertNull(s1.ejercicios().get(1).esfuerzo());
        assertTrue(s1.ejercicios().get(1).filas().isEmpty());
        assertNull(r.sesiones().get(1).sensacionFinal());
        assertTrue(r.sesiones().get(1).ejercicios().isEmpty());
    }

    @Test
    void idaYVueltaConClaveFinal() {
        var r = new RegistroCelular("rutineitor-registro", 1, "Franco", 9, List.of(
                new SesionRegistro(2, 3, "2026-11-04", 6, 1, List.of(new EjercicioRegistro("3-1", 5, Map.of("100", "1-150")))),
                new SesionRegistro(2, 1, "2026-11-02", null, null, List.of())));
        String json = JsonRegistro.registroAJson(r);
        var sesion = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("sesiones").get(0).getAsJsonObject();
        assertTrue(sesion.has("final"));
        assertFalse(sesion.has("sensacionFinal"));
        assertEquals(1, sesion.get("final").getAsInt());
        assertEquals(r, JsonRegistro.registroDeJson(json));
    }

    @Test
    void nullsSeNormalizan() {
        var r = JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"ejercicios\":null},"
                + "{\"microciclo\":1,\"dia\":2,\"fecha\":\"2026-10-28\",\"ejercicios\":[{\"bloque\":\"2-1\",\"esfuerzo\":2,\"filas\":null}]}]"));
        assertNotNull(r.sesiones().get(0).ejercicios());
        assertTrue(r.sesiones().get(0).ejercicios().isEmpty());
        assertNotNull(r.sesiones().get(1).ejercicios().get(0).filas());
        assertTrue(r.sesiones().get(1).ejercicios().get(0).filas().isEmpty());
        assertTrue(JsonRegistro.registroDeJson(
                "{\"formato\":\"rutineitor-registro\",\"version\":1,\"perfil\":\"F\",\"mesocicloId\":3,\"sesiones\":null}")
                .sesiones().isEmpty());
    }

    @Test
    void rangosValidos() {
        assertEquals(1, JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"descanso\":1,\"final\":5,"
                + "\"ejercicios\":[{\"bloque\":\"1-1\",\"esfuerzo\":5,\"filas\":{}}]}]")).sesiones().size());
        assertEquals(1, JsonRegistro.registroDeJson(con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"descanso\":6,\"final\":1,"
                + "\"ejercicios\":[{\"bloque\":\"1-1\",\"esfuerzo\":1,\"filas\":{}}]}]")).sesiones().size());
    }

    @Test
    void errores() {
        String[] malos = {
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"descanso\":7}]"),
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"descanso\":0}]"),
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"final\":6}]"),
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"ejercicios\":[{\"bloque\":\"1-1\",\"esfuerzo\":0}]}]"),
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\",\"ejercicios\":[{\"bloque\":\"1-1\",\"esfuerzo\":6}]}]"),
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"27/10/2026\"}]"),
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-02-30\"}]"),
                con("[{\"microciclo\":1,\"dia\":1}]"),
                con("[{\"microciclo\":0,\"dia\":1,\"fecha\":\"2026-10-27\"}]"),
                con("[{\"microciclo\":1,\"dia\":0,\"fecha\":\"2026-10-27\"}]"),
                con("[{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-27\"},{\"microciclo\":1,\"dia\":1,\"fecha\":\"2026-10-28\"}]"),
                "{\"formato\":\"rutineitor-plan\",\"version\":1,\"sesiones\":[]}",
                "{\"formato\":\"rutineitor-registro\",\"version\":2,\"sesiones\":[]}",
                "{ roto",
        };
        for (String m : malos) {
            assertThrows(IllegalArgumentException.class, () -> JsonRegistro.registroDeJson(m), m);
        }
    }

    @Test
    void elPlanSigueFuncionando() throws Exception {
        // agregar el registro no tiene que romper la lectura del plan de T04
        var p = JsonRegistro.planDeJson(Files.readString(Path.of("docs/registro-sesiones/ejemplos/plan-ejemplo.json")));
        assertEquals(4, p.sesiones().size());
    }
}
