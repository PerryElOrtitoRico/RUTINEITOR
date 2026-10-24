# T03 — Armar el plan para el celular

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, `mvn -q test` en verde al terminar.

## Objetivo
A partir de un mesociclo ya calculado, armar el objeto `PlanCelular` que después (T04) se guarda como el JSON de la **sección 5 de la especificación**. Las reglas de las filas están en la **sección 4**: leelas completas.

## Código que ya existe (no lo modifiques)
```java
// com.rutina.model
public enum Tipo { POTENCIA, FUERZA, HIPERTROFIA, PLIOMETRIA }
public enum Aproximacion { COMPLEJA, BASICA }
public enum Zona { Z55(55), Z60(60), Z65(65), Z70(70), Z75(75), Z80(80), Z85(85), Z90(90), Z95(95), Z100(100);
    public int pct(); }
public record Bloque(int dia, int posicion, long ejercicioId, double pctBolsa) { }
public record Ejercicio(long id, long perfilId, String nombre, Tipo tipo, Double rm, Double rmObjetivo,
        int techoPct, Map<Zona, Integer> repsPorSerie, String grupoPlio, int orden, boolean activo) { }
public record CeldaPlantilla(int series, int reps, Integer pctMin, Integer pctMax) {
    public String texto(); }          // "4x12", "3x3-90%", "3x5-75/80%"
public record Mesociclo(long id, long perfilId, LocalDate inicio, int microciclos, ParametrosTipo potencia,
        ParametrosTipo fuerza, Character variante, Integer dias, Map<Long, Aproximacion> aproximaciones,
        List<Bloque> bloques, String calentamiento, String auxiliares, Map<Long, Double> rmUsados) { }
public record Metodologia(/* … */ double incrementoKg) { }   // usá solo m.incrementoKg()

// com.rutina.service.calculo
public final class Redondeo {
    /** kg = rm × pct / 100, redondeado al múltiplo de incremento. */
    public static double kg(double rm, double pct, double incremento); }

public record ResultadoMesociclo(List<ResultadoBloque> bloques, /* … */) {
    /** Un microciclo de un bloque.
     *  Compleja: seriesPorZona tiene TODAS las zonas; cada valor es una lista con UNA lista de tamaños de serie
     *  (vacía si no se trabaja esa zona). Ejemplo: Z80 → [[3, 3, 3]]. celda == null.
     *  Básica/hipertrofia: celda != null y seriesPorZona vacío. */
    public record SemanaBloque(int volumen, Map<Zona, List<List<Integer>>> seriesPorZona, CeldaPlantilla celda) { }
    /** semanas.get(w) = microciclo w + 1. */
    public record ResultadoBloque(Bloque bloque, Ejercicio ejercicio, Aproximacion aprox, List<SemanaBloque> semanas) { }
}
```
`res.bloques()` trae un `ResultadoBloque` por cada bloque válido del mesociclo; `aprox` ya viene resuelto (hipertrofia = `BASICA`).

## Archivos
- Crear en `src/main/java/com/rutina/registro/`: `FilaPlan.java`, `EjercicioPlan.java`, `SesionPlan.java`, `PlanCelular.java`, `ArmadorPlan.java`
- Crear `src/test/java/com/rutina/registro/ArmadorPlanTest.java`

## Qué escribir
```java
public record FilaPlan(String etiqueta, Integer pctMin, Integer pctMax, List<SerieReal> series) { }
public record EjercicioPlan(String bloque, String nombre, Tipo tipo, Aproximacion modo, Double rm, List<FilaPlan> filas) { }
public record SesionPlan(int microciclo, int dia, List<EjercicioPlan> ejercicios) { }
public record PlanCelular(String formato, int version, String perfil, long mesocicloId, String inicio,
                          int microciclos, int dias, List<SesionPlan> sesiones) {
    public static final String FORMATO = "rutineitor-plan";
    public static final int VERSION = 1;
}

public final class ArmadorPlan {
    private ArmadorPlan() { }
    /** mes.dias() no es null (el mesociclo ya se calculó). */
    public static PlanCelular armar(String perfil, Mesociclo mes, ResultadoMesociclo res, Metodologia m) { ... }
}
```
`SerieReal` ya existe (T02). Los nombres de los componentes de los records **tienen que ser exactamente estos**, porque en T04 Gson los usa como claves del JSON.

## Reglas
1. Una `SesionPlan` por cada microciclo `1..mes.microciclos()` y cada día `1..mes.dias()`, en ese orden (todos los días del microciclo 1, después el 2…), aunque quede sin ejercicios.
2. Ejercicios de la sesión (micro, día): los `ResultadoBloque` con `bloque().dia() == día`, ordenados por `bloque().posicion()`. Para cada uno se usa `semanas().get(micro - 1)`.
3. `bloque` = `dia + "-" + posicion`. `inicio` = `mes.inicio().toString()` (formato `2026-10-26`).
4. `rm` = `mes.rmUsados().getOrDefault(ejercicio.id(), ejercicio.rm())`.
5. Filas de `COMPLEJA`: la tabla de la sección 4. Solo se agregan las filas con al menos una serie. Cada serie lleva el kg de **su** zona: `rm == null ? 0 : Redondeo.kg(rm, zona.pct(), m.incrementoKg())`.
6. Filas de `BASICA`: una sola fila, `etiqueta = celda.texto()`, `pctMin`/`pctMax` de la celda, `celda.series()` series de `celda.reps()` reps. kg = `Redondeo.kg(rm, (pctMin + pctMax) / 2.0, inc)` si `rm != null` y `pctMin != null`; si no, 0.
7. Listas inmutables (`List.of`, `List.copyOf` o `.toList()`).

## Tests a escribir (como mínimo)
Armá un mesociclo y calculalo con la calculadora real:
```java
var M = com.rutina.service.datos.DatosIniciales.metodologia();          // incrementoKg = 2.5
var sentadilla = new Ejercicio(1, 1, "Sentadilla", Tipo.FUERZA, 150.0, null, 100, Map.of(), null, 0, true);
var biceps = new Ejercicio(2, 1, "Biceps", Tipo.HIPERTROFIA, null, null, 100, Map.of(), null, 0, true);
var mes = new Mesociclo(7, 1, LocalDate.of(2026, 10, 26), 2, new ParametrosTipo(0, 72), new ParametrosTipo(200, 70),
        'A', 3, Map.of(1L, Aproximacion.COMPLEJA), List.of(new Bloque(1, 1, 1L, 100), new Bloque(2, 1, 2L, 100)),
        "", "", Map.of());
var res = com.rutina.service.calculo.CalculadoraMesociclo.calcular(M, mes, Map.of(1L, sentadilla, 2L, biceps));
var plan = ArmadorPlan.armar("Franco", mes, res, M);
```
Y comprobá:
- `plan.sesiones().size() == 6` y la tercera es `(microciclo 1, día 3)`, sin ejercicios.
- En la sesión (1, 1), el ejercicio tiene `bloque "1-1"`, `modo COMPLEJA` y `rm 150.0`.
- La fila `"80-85"` de esa sesión tiene como series, en orden, las de `Z80` y después las de `Z85` de `res.bloques().get(0).semanas().get(0)`. Las series del 80 % tienen `kg 120.0` y las del 85 %, `127.5`.
- En la sesión (2, 2), el bíceps tiene una sola fila con `etiqueta "4x10"`, 4 series de 10 reps y `kg 0.0`.

## Cómo verificar
`mvn -q test` → sin errores.

## No hagas
- No recalcules el mesociclo dentro de `ArmadorPlan`: usá el `ResultadoMesociclo` que recibís.
- No incluyas filas sin series.
