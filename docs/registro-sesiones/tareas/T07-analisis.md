# T07 — Comparar lo planificado con lo realizado

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, `mvn -q test` en verde al terminar.

## Objetivo
Con el plan (T03) y el registro (T05), calcular por cada sesión anotada el volumen y la intensidad planificados y reales, como dice la **sección 7 de la especificación**. Leela completa.

## Código que ya existe
- `PlanCelular`, `SesionPlan`, `EjercicioPlan(bloque, nombre, tipo, modo, rm, filas)` y `FilaPlan(etiqueta, pctMin, pctMax, series)` (T03).
- `RegistroCelular`, `SesionRegistro(microciclo, dia, fecha, descanso, sensacionFinal, ejercicios)` y `EjercicioRegistro(bloque, esfuerzo, filas)` (T05).
- `NotacionRegistro.parse(texto, planificadas)` y `SerieReal(reps, kg)` (T02).
- `com.rutina.model.Tipo { POTENCIA, FUERZA, HIPERTROFIA, PLIOMETRIA }`.

## Archivos
- Crear en `src/main/java/com/rutina/registro/`: `ComparacionTipo.java`, `ResumenSesion.java`, `AnalisisRegistro.java`
- Crear `src/test/java/com/rutina/registro/AnalisisRegistroTest.java`

## Qué escribir
```java
public record ComparacionTipo(Tipo tipo, int volumenPlan, int volumenReal, Double imrPlan, Double imrReal) { }

public record ResumenSesion(int microciclo, int dia, String fecha, Integer descanso, Integer sensacionFinal,
                            Double esfuerzoPromedio, List<ComparacionTipo> porTipo) { }

public final class AnalisisRegistro {
    private AnalisisRegistro() { }
    /**
     * Un ResumenSesion por cada sesión del registro que exista en el plan, ordenados por (microciclo, día).
     * @throws IllegalArgumentException si plan y registro son de distinto mesocicloId, o si una casilla tiene
     *         una notación inválida (el mensaje dice microciclo, día, bloque y etiqueta).
     */
    public static List<ResumenSesion> analizar(PlanCelular plan, RegistroCelular registro) { ... }
}
```

## Reglas
1. Las sesiones del registro que no existen en el plan se ignoran.
2. Para cada ejercicio del plan en esa sesión y cada fila suya: si el registro tiene, para ese `bloque`, un texto no vacío en `filas.get(etiqueta)`, las series reales son `NotacionRegistro.parse(texto, fila.series())`; si no, son las planificadas. Un ejercicio que no aparece en el registro = todo como lo planificado.
3. `porTipo`: una `ComparacionTipo` por cada `Tipo` que tenga ejercicios en esa sesión del plan, en el orden del enum (`POTENCIA`, `FUERZA`, `HIPERTROFIA`).
4. Volumen = Σ reps. IMR = Σ(reps × intensidad) / Σ reps, contando solo las series con intensidad conocida:
   - si `rm != null` y `kg > 0` → intensidad = `kg / rm * 100`;
   - si no, y la fila tiene `pctMin` y `pctMax` → `(pctMin + pctMax) / 2.0`;
   - si no, la serie no cuenta para el IMR (sí para el volumen).
   - Si ninguna serie cuenta → IMR `null`.
5. `esfuerzoPromedio` = promedio de los `esfuerzo` no nulos de los ejercicios del registro en esa sesión; `null` si no hay ninguno.

## Tests a escribir (como mínimo)
Leé los dos ejemplos (`docs/registro-sesiones/ejemplos/plan-ejemplo.json` y `registro-ejemplo.json`) con `JsonRegistro` y llamá a `analizar`. Resultado esperado:

**Sesión (1, 1)**: fecha `2026-10-27`, descanso 5, final 4, `esfuerzoPromedio 4.0`.
- `FUERZA`: plan 17, real 14, IMR plan `1295 / 17 = 76.176…`, IMR real `1030 / 14 = 73.571…` (tolerancia `1e-9`).
  - Planificado: fila 70 → 4 + 4 reps al 70 %; fila 80-85 → 3 + 3 al 80 % y 3 al 85 %.
  - Real: la fila 80-85 dice `3.3-115` → 3 reps a 120 kg (80 %) y 3 a 115 kg (76,666… %).
- `HIPERTROFIA`: plan 48, real 48, ambos IMR `null`.

**Sesión (1, 2)**: fecha `2026-10-29`, descanso 2, final `null`, `esfuerzoPromedio null`.
- `FUERZA`: plan 24, real 24, ambos IMR `90 / 130 * 100 = 69.2307…`.

Además:
- Si el registro tiene otro `mesocicloId`, se lanza `IllegalArgumentException`.
- Si una casilla dice `3..3`, se lanza `IllegalArgumentException` cuyo mensaje contiene `1-1` y `80-85`.

## Cómo verificar
`mvn -q test` → sin errores.
