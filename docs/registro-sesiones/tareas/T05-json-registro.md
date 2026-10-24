# T05 — Leer el registro que viene del celular

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, `mvn -q test` en verde al terminar.

## Objetivo
Leer y validar el archivo de la **sección 6 de la especificación** (ejemplo: `docs/registro-sesiones/ejemplos/registro-ejemplo.json`).

## Código que ya existe
- `JsonRegistro` (T04), con su `Gson` privado. Agregá los métodos nuevos ahí.
- Las escalas de T01: `EscalaDescanso` (1–6), `EscalaEsfuerzo` (1–5) y `EscalaFinal` (1–5).

## Archivos
- Crear en `src/main/java/com/rutina/registro/`: `EjercicioRegistro.java`, `SesionRegistro.java`, `RegistroCelular.java`
- Modificar `src/main/java/com/rutina/registro/JsonRegistro.java` (solo para agregar métodos)
- Crear `src/test/java/com/rutina/registro/JsonRegistroTest.java`

## Qué escribir
```java
public record EjercicioRegistro(String bloque, Integer esfuerzo, Map<String, String> filas) { }

public record SesionRegistro(int microciclo, int dia, String fecha, Integer descanso,
                             @SerializedName("final") Integer sensacionFinal,
                             List<EjercicioRegistro> ejercicios) { }

public record RegistroCelular(String formato, int version, String perfil, long mesocicloId,
                              List<SesionRegistro> sesiones) {
    public static final String FORMATO = "rutineitor-registro";
    public static final int VERSION = 1;
}
```
En el JSON la clave es `"final"`, que en Java es palabra reservada. Por eso el componente se llama `sensacionFinal` y lleva `@SerializedName("final")` (`com.google.gson.annotations.SerializedName`).

En `JsonRegistro`:
```java
/** @throws IllegalArgumentException si el archivo no es un registro válido (ver Reglas). */
public static RegistroCelular registroDeJson(String json) { ... }
public static RegistroCelular leerRegistro(java.nio.file.Path archivo) throws java.io.IOException { ... }
public static String registroAJson(RegistroCelular r) { ... }   // lo usa T06 para guardarlo en la base
```

## Reglas de validación (cada una con un mensaje en español que diga qué falló)
1. `formato` igual a `"rutineitor-registro"` y `version` igual a `1`.
2. Cada sesión: `microciclo >= 1`, `dia >= 1`, y `fecha` en formato `AAAA-MM-DD` válido (`LocalDate.parse`).
3. `descanso` null o entre 1 y 6; `sensacionFinal` null o entre 1 y 5; `esfuerzo` null o entre 1 y 5.
4. No puede haber dos sesiones con el mismo (microciclo, día).
5. Normalización: si `sesiones`, `ejercicios` o `filas` vienen null en el JSON, se devuelven como lista o mapa vacío. El resultado tiene que tener colecciones no nulas.
6. JSON mal formado → `IllegalArgumentException`.

## Tests a escribir (como mínimo)
- Leer el ejemplo: 2 sesiones. La primera tiene `descanso 5`, `sensacionFinal 4`, y su ejercicio `"1-1"` tiene `esfuerzo 4` y `filas.get("80-85") == "3.3-115"`. La segunda tiene `sensacionFinal null` y `ejercicios` vacío.
- Ida y vuelta: `registroDeJson(registroAJson(r))` es igual a `r`, y el JSON contiene `"final"` (no `"sensacionFinal"`).
- Errores: descanso 7, esfuerzo 0, fecha `"27/10/2026"`, dos sesiones (1, 1) y formato `"rutineitor-plan"` lanzan `IllegalArgumentException`.
- Un registro con `"ejercicios": null` se lee con `ejercicios()` vacío.

## Cómo verificar
`mvn -q test` → sin errores.
