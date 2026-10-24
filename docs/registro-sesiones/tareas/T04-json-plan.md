# T04 — Plan en JSON

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, `mvn -q test` en verde al terminar.

## Objetivo
Convertir el `PlanCelular` de T03 a JSON y leerlo de vuelta. El formato es el de la **sección 5 de la especificación**, y hay un ejemplo real en `docs/registro-sesiones/ejemplos/plan-ejemplo.json`.

## Código que ya existe
- `PlanCelular`, `SesionPlan`, `EjercicioPlan`, `FilaPlan` y `SerieReal` (T02 y T03), en `com.rutina.registro`.
- Gson 2.10.1 ya está en el `pom.xml`, y la versión 2.10 serializa records.

## Archivos
- Crear `src/main/java/com/rutina/registro/JsonRegistro.java`
- Crear `src/test/java/com/rutina/registro/JsonPlanTest.java`

## Qué escribir
```java
package com.rutina.registro;

/** Lectura y escritura de los archivos que van y vienen del celular. */
public final class JsonRegistro {
    private JsonRegistro() { }

    /** JSON con sangría; los null se escriben (por ejemplo "rm": null). */
    public static String planAJson(PlanCelular plan) { ... }

    /** @throws IllegalArgumentException si no es un plan válido: formato distinto de "rutineitor-plan",
     *  version distinta de 1, o JSON mal formado (mensaje en español). */
    public static PlanCelular planDeJson(String json) { ... }

    /** Escribe en UTF-8. */
    public static void escribirPlan(java.nio.file.Path destino, PlanCelular plan) throws java.io.IOException { ... }
}
```

## Reglas
- Usá **un** `Gson` propio de esta clase, creado con `new GsonBuilder().setPrettyPrinting().serializeNulls().create()`. No uses el de `Database`: es privado de otro paquete.
- `tipo` y `modo` se escriben como texto (`"FUERZA"`, `"COMPLEJA"`); es lo que Gson hace por defecto con los enums.
- `planDeJson` atrapa `JsonParseException` y la convierte en `IllegalArgumentException`.

## Tests a escribir (como mínimo)
- **Ida y vuelta:** armá un `PlanCelular` chico a mano (1 sesión, 1 ejercicio, 1 fila con 2 series) y comprobá que `planDeJson(planAJson(p))` es igual (`assertEquals`) a `p`.
- **JSON con las claves de la especificación:** `planAJson(p)` contiene `"formato": "rutineitor-plan"`, `"mesocicloId"`, `"etiqueta"` y `"rm": null` (para un ejercicio sin rm).
- **Lectura del ejemplo:** leé `docs/registro-sesiones/ejemplos/plan-ejemplo.json` con `Files.readString(Path.of(...))` y comprobá que tiene 4 sesiones, y que la fila `"80-85"` de la primera sesión tiene 3 series, con la última a `127.5` kg.
- **Errores:** `planDeJson("{\"formato\":\"otro\",\"version\":1}")`, `planDeJson("{")` y un plan con `"version": 2` lanzan `IllegalArgumentException`.

## Cómo verificar
`mvn -q test` → sin errores.
