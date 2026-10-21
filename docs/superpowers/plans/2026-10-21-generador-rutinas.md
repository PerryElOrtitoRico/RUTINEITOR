# Generador de rutinas — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Objetivo:** aplicación de escritorio que, a partir de la planificación anual y del método cubano, genera series × repeticiones × kg de cada mesociclo y exporta el PDF para el gimnasio.

**Arquitectura:** el núcleo es un motor de cálculo puro (`service.calculo`), sin interfaz ni base de datos, testeado contra los valores del Excel. Alrededor van la persistencia (SQLite: tablas para perfiles, ejercicios, mesociclos y planes; la metodología se guarda como JSON con Gson), el exportador PDF y la interfaz Swing. Las series **no se guardan**: se recalculan cada vez.

**Stack:** Java 21, Maven, Swing + FlatLaf 3.4.1, sqlite-jdbc 3.45.1.0, Gson 2.10.1, OpenPDF (última 1.x, paquete `com.lowagie`), JFreeChart 1.5.5, JUnit 5.10.2.

**Spec:** `docs/2026-10-21-generador-rutinas-design.md` (en el plan, "spec §N" se refiere a sus secciones). Leer también `../CLAUDE.md` (convenciones del usuario).

## Restricciones globales

- `maven.compiler.release` 21; paquete base `com.rutina`; subpaquetes `model`, `service` (y `service.calculo`, `service.datos`), `pdf`, `ui`.
- Identificadores del dominio en español (`Ejercicio`, `Mesociclo`, `Zona`…); comentarios, README y mensajes de la interfaz en español.
- Commits: `feat:`/`test:`/`docs:`… + descripción en español. Uno por tarea.
- Datos: `$XDG_DATA_HOME/rutina/rutina.db`, o `~/.local/share/rutina/rutina.db` si la variable no está definida. Nunca rutas relativas ni `user.dir`.
- Ejecutable: `maven-assembly-plugin` `jar-with-dependencies`, `mainClass` `com.rutina.Main`.
- Zonas de cálculo: 55, 60, 65, 70, 75, 80, 85, 90, 95, 100 % (10 zonas). El agrupamiento 80–87,5 / 90–95 es **solo** del PDF.
- Redondeo: `HALF_UP` en todos los casos. En el método del mayor resto, a igual fracción gana el índice menor.
- La lógica no va dentro de las clases de `ui`.
- Nuevas dependencias respecto de los otros proyectos del usuario: OpenPDF y JFreeChart. Ya están justificadas en el spec §4; no agregar ninguna otra.

## Review Focus

1. **IMR efectivo fuera de 60–80**: por ejemplo, fuerza a 79 % con 5A da 82 % en el microciclo 5. Se espera un mensaje que nombre el microciclo y el valor, sin excepción sin capturar en la interfaz. → test en la Tarea 3.
2. **Ejercicio sin 1RM** (dominadas, auxiliares): el cálculo y el PDF tienen que funcionar con el kg vacío. → tests en las Tareas 5 y 9.
3. **Volumen de un tipo sin ningún ejercicio complejo** en el microciclo, o con todos los ejercicios con techo por debajo de una zona: todo va a "sin asignar" en los contadores, sin dividir por cero. → tests en las Tareas 4 y 8.
4. **Combinación de variante inexistente** (5C, 2C) o mesociclo sin variante o sin días todavía: error claro, no `NullPointerException`. → tests en las Tareas 3 y 8.
5. **Base de datos ilegible** (archivo corrupto): se respalda como `rutina.db.bak-<timestamp>` y se crea una nueva, con aviso. Nunca se pisa sin respaldar. → test en la Tarea 10.

---

### Tarea 1: Proyecto Maven, enums base y redondeo

**Files:**
- Create: `pom.xml`, `.gitignore` (`target/`, `*.db`, `.idea/`, `.vscode/`, `*.iml`)
- Create: `src/main/java/com/rutina/model/Tipo.java`, `Zona.java`
- Create: `src/main/java/com/rutina/service/calculo/Redondeo.java`
- Test: `src/test/java/com/rutina/service/calculo/RedondeoTest.java`

**Interfaces:**
- Produces:
  - `enum Tipo { POTENCIA, FUERZA, HIPERTROFIA, PLIOMETRIA }`
  - `enum Zona { Z55..Z100 }` con `int pct()`, `static Zona dePct(int)` (lanza `IllegalArgumentException` si no existe).
  - `Redondeo.halfUp(double) -> int`
  - `Redondeo.mayorResto(int total, double[] pesos) -> int[]`: reparte `total` según los pesos y suma exactamente `total`. Si todos los pesos son 0, devuelve todo 0.
  - `Redondeo.kg(double rm, double pct, double incremento) -> double`: `rm × pct / 100`, redondeado al múltiplo de `incremento` más cercano (`HALF_UP`).

- [ ] **Paso 1: tests que fallan**

```java
@Test void mayorRestoEjemploLibro() { // 163 reps, fila IMR 74 de Ref
    assertArrayEquals(new int[]{13,16,18,20,21,36,29,10},
        Redondeo.mayorResto(163, new double[]{8,10,11,12,13,22,18,6}));
}
@Test void mayorRestoEmpateGanaIndiceMenor() {
    assertArrayEquals(new int[]{74,80,91,63,42}, Redondeo.mayorResto(350, new double[]{21,23,26,18,12}));
}
@Test void mayorRestoPesosCero() { assertArrayEquals(new int[]{0,0}, Redondeo.mayorResto(10, new double[]{0,0})); }
@Test void kgRedondea() {
    assertEquals(88.0, Redondeo.kg(100, 87.5, 1.0));
    assertEquals(92.5, Redondeo.kg(115, 80, 2.5));
}
@Test void halfUp() { assertEquals(3, Redondeo.halfUp(2.5)); assertEquals(4, Redondeo.halfUp(250 / 60.0)); assertEquals(6, Redondeo.halfUp(350 / 60.0)); }
```

- [ ] **Paso 2:** `mvn test`. Esperado: no compila o falla.
- [ ] **Paso 3:** escribir el `pom.xml` (dependencias y plugins del stack, surefire 3.2.5, assembly 3.6.0 como `PlanificadorEstudio/pom.xml`), los enums y `Redondeo`.
- [ ] **Paso 4:** `mvn test`. Esperado: BUILD SUCCESS.
- [ ] **Paso 5:** commit `chore: proyecto Maven y redondeo base`.

### Tarea 2: Modelo de metodología y datos iniciales

**Files:**
- Create: `src/main/java/com/rutina/model/{Metodologia,VarianteMicro,CeldaPlantilla,ClavePlantilla,Plantilla}.java`
- Create: `src/main/java/com/rutina/service/datos/DatosIniciales.java`
- Test: `src/test/java/com/rutina/service/datos/DatosInicialesTest.java`, `src/test/java/com/rutina/model/CeldaPlantillaTest.java`

**Interfaces:**
- Produces:
  - `record VarianteMicro(double[] volPct, int[] deltaImr)`
  - `record CeldaPlantilla(int series, int reps, Integer pctMin, Integer pctMax)`, con `static CeldaPlantilla parse(String)` y `String texto()`, que devuelve el formato original: `"4x12"`, `"3x3-90%"`, `"3x5-75/80%"`.
  - `record ClavePlantilla(Tipo tipo, int semanas, int diasPorSemana)`
  - `record Plantilla(List<List<CeldaPlantilla>> semanas, boolean inferida)`: `semanas.get(w).get(k)` es la celda de la aparición k en la semana w.
  - `record Metodologia(Map<String,VarianteMicro> variantes, Map<Integer,double[]> diaPct, Map<Integer,double[]> ref, Map<Tipo,int[]> repsPorSerie, Map<ClavePlantilla,Plantilla> plantillas, Map<Integer,int[][]> volPlio, double incrementoKg)`
    - `variantes`: clave `"5A"`…
    - `diaPct`: clave = días (3, 4, 5).
    - `ref`: clave = IMR (60–80); el valor tiene 10 pesos, uno por zona.
    - `repsPorSerie`: 10 valores por tipo.
    - `volPlio`: por nivel, `[semana][día]`.
    - `incrementoKg`: 1.0 por defecto.
  - `DatosIniciales.metodologia() -> Metodologia`: todo el contenido de spec §11.1 a §11.5, con las filas 65 y 66 de Ref corregidas a `{15,20,30,20,15,…}` y `{14,18,30,18,12,8,…}`. Las reps/serie por defecto, para POTENCIA y FUERZA, son `{4,4,4,4,3,3,3,2,1,1}`. Las plantillas inferidas (3, 4 y 5 días) se agregan en la Tarea 6.
  - `DatosIniciales.ejercicios() -> List<Ejercicio>`: se define en la Tarea 7 (depende de `Ejercicio`).

- [ ] **Paso 1: tests que fallan**

```java
@Test void parseCeldas() {
    assertEquals(new CeldaPlantilla(3,5,75,80), CeldaPlantilla.parse("3x5-75/80%"));
    assertEquals(new CeldaPlantilla(3,3,90,90), CeldaPlantilla.parse("3x3-90%"));
    assertEquals(new CeldaPlantilla(4,12,null,null), CeldaPlantilla.parse("4x12"));
    assertEquals("3x5-75/80%", CeldaPlantilla.parse("3x5-75/80%").texto());
}
@Test void variantesSuman100() { DatosIniciales.metodologia().variantes().forEach((k,v) ->
    assertEquals(100.0, Arrays.stream(v.volPct()).sum(), 1e-9, k)); }
@Test void hayDiezVariantes() { assertEquals(Set.of("5A","5B","4A","4B","4C","3A","3B","3C","2A","2B"),
    DatosIniciales.metodologia().variantes().keySet()); }
@Test void refFilas65y66Corregidas() { var r = DatosIniciales.metodologia().ref();
    assertEquals(100.0, Arrays.stream(r.get(65)).sum(), 1e-9);
    assertEquals(100.0, Arrays.stream(r.get(66)).sum(), 1e-9);
    assertEquals(120.0, Arrays.stream(r.get(60)).sum(), 1e-9); } // errata del libro: se conserva
@Test void plantillaFuerza3Semanas2Dias() { var p = DatosIniciales.metodologia().plantillas()
        .get(new ClavePlantilla(Tipo.FUERZA,3,2));
    assertEquals("3x6-70/75%", p.semanas().get(1).get(0).texto()); assertFalse(p.inferida()); }
@Test void volPlioNivel3() { assertArrayEquals(new int[]{350,350}, DatosIniciales.metodologia().volPlio().get(3)[3]); }
```

- [ ] **Paso 2:** `mvn test -Dtest='DatosInicialesTest,CeldaPlantillaTest'`. Esperado: FAIL.
- [ ] **Paso 3:** implementar. Los valores se copian de las tablas de spec §11; las plantillas de hipertrofia y de fuerza básica (1 y 2 días, 2–5 semanas) salen de §11.5.
- [ ] **Paso 4:** mismo comando. Esperado: PASS.
- [ ] **Paso 5:** commit `feat: metodología y datos iniciales del Excel`.

### Tarea 3: Distribución del volumen por microciclo y por día (spec §6.1)

**Files:**
- Create: `src/main/java/com/rutina/model/ParametrosTipo.java`, `src/main/java/com/rutina/service/calculo/{DistribucionVolumen,ParametrosInvalidosException}.java`
- Test: `src/test/java/com/rutina/service/calculo/DistribucionVolumenTest.java`

**Interfaces:**
- Consumes: `Metodologia`, `Redondeo.mayorResto`.
- Produces:
  - `record ParametrosTipo(int volumen, int imr)`
  - `record PlanMicro(int volumen, int imr)` (en `service.calculo`)
  - `DistribucionVolumen.porMicrociclo(Metodologia m, ParametrosTipo p, int microciclos, char variante) -> List<PlanMicro>`: los volúmenes se reparten con `mayorResto(p.volumen(), volPct)`. Lanza `ParametrosInvalidosException` (checked, con mensaje en español) si la variante no existe o si algún IMR efectivo no está en `m.ref()`.
  - `DistribucionVolumen.pesosDias(Metodologia m, int dias, Set<Integer> diasConTipo) -> double[]`: largo `dias`, días indexados desde 1. Los días ausentes van en 0 y el resto se reescala para sumar 1. Si el conjunto está vacío, todo 0.

- [ ] **Paso 1: tests que fallan**

```java
@Test void fuerza5A() throws Exception {
    assertEquals(List.of(new PlanMicro(74,76),new PlanMicro(80,75),new PlanMicro(91,74),new PlanMicro(63,78),new PlanMicro(42,79)),
        DistribucionVolumen.porMicrociclo(M, new ParametrosTipo(350,76), 5, 'A'));
}
@Test void imrFueraDeTabla() { var e = assertThrows(ParametrosInvalidosException.class,
    () -> DistribucionVolumen.porMicrociclo(M, new ParametrosTipo(350,79), 5, 'A'));
    assertTrue(e.getMessage().contains("82")); assertTrue(e.getMessage().contains("5")); }
@Test void varianteInexistente() { assertThrows(ParametrosInvalidosException.class,
    () -> DistribucionVolumen.porMicrociclo(M, new ParametrosTipo(300,72), 5, 'C')); }
@Test void pesosDiasReescalados() { assertArrayEquals(new double[]{0.35/0.55, 0, 0.20/0.55},
    DistribucionVolumen.pesosDias(M, 3, Set.of(1,3)), 1e-9); }
@Test void unSoloDiaSeLlevaTodo() { assertArrayEquals(new double[]{0,0,1,0},
    DistribucionVolumen.pesosDias(M, 4, Set.of(3)), 1e-9); }
```
(`M = DatosIniciales.metodologia()`)

- [ ] **Paso 2:** `mvn test -Dtest=DistribucionVolumenTest`. FAIL.
- [ ] **Paso 3:** implementar.
- [ ] **Paso 4:** mismo comando. PASS.
- [ ] **Paso 5:** commit `feat: distribución de volumen por microciclo y día`.

### Tarea 4: Repeticiones por zona y reparto entre ejercicios con techo (spec §6.2)

**Files:**
- Create: `src/main/java/com/rutina/service/calculo/{DistribucionZonas,RepartoEjercicios}.java`
- Test: `src/test/java/com/rutina/service/calculo/{DistribucionZonasTest,RepartoEjerciciosTest}.java`

**Interfaces:**
- Produces:
  - `DistribucionZonas.repsPorZona(Metodologia m, int volumen, int imr) -> int[]` (10 posiciones): `mayorResto(volumen, m.ref().get(imr))`. Como `mayorResto` reparte proporcionalmente, la normalización de la fila sale sola.
  - `DistribucionZonas.repartirEnDias(int reps, double[] pesosDias) -> int[]`: `mayorResto`.
  - `record Participante(long ejercicioId, double pct, int techoPct)`
  - `record ResultadoReparto(Map<Long,Integer> porEjercicio, int sinAsignar)`
  - `RepartoEjercicios.repartir(int reps, Zona z, List<Participante> ps) -> ResultadoReparto`: participan solo los que tienen `techoPct >= z.pct()`. Las reps se reparten con `mayorResto` según `pct`, así que los % no tienen que sumar 100. Si no participa nadie, todo va a `sinAsignar`.

- [ ] **Paso 1: tests que fallan**

```java
@Test void ejemploLibro() { assertArrayEquals(new int[]{13,16,18,20,21,36,29,10,0,0}, DistribucionZonas.repsPorZona(M,163,74)); }
@Test void fila60SeNormaliza() { assertArrayEquals(new int[]{46,33,13,8,0,0,0,0,0,0}, DistribucionZonas.repsPorZona(M,100,60)); }
@Test void techoPasaAlOtro() { var r = RepartoEjercicios.repartir(10, Zona.Z90,
        List.of(new Participante(1,50,100), new Participante(2,50,85)));
    assertEquals(Map.of(1L,10), r.porEjercicio()); assertEquals(0, r.sinAsignar()); }
@Test void mitadYMitad() { assertEquals(Map.of(1L,5,2L,4), RepartoEjercicios.repartir(9, Zona.Z60,
        List.of(new Participante(1,50,100), new Participante(2,50,100))).porEjercicio()); }
@Test void nadieAdmiteLaZona() { var r = RepartoEjercicios.repartir(4, Zona.Z95, List.of(new Participante(2,100,85)));
    assertEquals(4, r.sinAsignar()); assertTrue(r.porEjercicio().isEmpty()); }
@Test void sinParticipantes() { assertEquals(7, RepartoEjercicios.repartir(7, Zona.Z55, List.of()).sinAsignar()); }
```

- [ ] **Paso 2–4:** `mvn test -Dtest='DistribucionZonasTest,RepartoEjerciciosTest'`: primero FAIL, después de implementar, PASS.
- [ ] **Paso 5:** commit `feat: repeticiones por zona y techo de intensidad`.

### Tarea 5: De repeticiones a series, con arrastre (spec §6.3)

**Files:**
- Create: `src/main/java/com/rutina/service/calculo/GeneradorSeries.java`
- Test: `src/test/java/com/rutina/service/calculo/GeneradorSeriesTest.java`

**Interfaces:**
- Produces:
  - `record SeriesZona(List<List<Integer>> porDia, int desvio)`: cada elemento de `porDia` es la lista de tamaños de serie de ese día; `desvio` = Σ series − Σ reps planificadas.
  - `GeneradorSeries.generar(int[] repsPorDia, int repsPorSerie) -> SeriesZona`
  - `GeneradorSeries.notacion(List<Integer> series) -> String`: `[3,3,3]` → `"3.3.3"`; una lista vacía → `""`.

Algoritmo (no se deduce de la firma):
```
arrastre = 0
para cada día d:
  r = repsPorDia[d] + arrastre
  si r <= 0: día sin series; arrastre = r
  si no:
    n = halfUp(r / s)
    si n == 0: una serie de r; arrastre = 0
    si no: n series de s; arrastre = r - n*s
desvio = Σ series − Σ repsPorDia
```

- [ ] **Paso 1: tests que fallan**

```java
@Test void arrastreCompensa() { var s = GeneradorSeries.generar(new int[]{4,4,4}, 3);
    assertEquals(List.of(List.of(3), List.of(3,3), List.of(3)), s.porDia()); assertEquals(0, s.desvio()); }
@Test void pocasReps() { var s = GeneradorSeries.generar(new int[]{1,0,1}, 4);
    assertEquals(List.of(List.of(1), List.of(), List.of(1)), s.porDia()); }
@Test void arrastreNegativoNoGeneraSeries() { var s = GeneradorSeries.generar(new int[]{5,0,0}, 3);
    assertEquals(List.of(List.of(3,3), List.of(), List.of()), s.porDia()); assertEquals(1, s.desvio()); }
@Test void notacion() { assertEquals("3.3.3.3", GeneradorSeries.notacion(List.of(3,3,3,3))); }
```

- [ ] **Paso 2–4:** `mvn test -Dtest=GeneradorSeriesTest`: FAIL → implementar → PASS.
- [ ] **Paso 5:** commit `feat: generación de series con arrastre`.

### Tarea 6: Plantillas básicas e hipertrofia, con inferencia (spec §6.4)

**Files:**
- Create: `src/main/java/com/rutina/service/calculo/Plantillas.java`
- Modify: `DatosIniciales.metodologia()` para que agregue las plantillas inferidas.
- Test: `src/test/java/com/rutina/service/calculo/PlantillasTest.java`

**Interfaces:**
- Produces:
  - `Plantillas.inferir(Plantilla dosDias, Tipo tipo, int dias) -> Plantilla` (`inferida = true`). Para cada semana, a partir de `[mod, pes]` de la plantilla de 2 días se arma `liv = escalonAbajo(mod, tipo)`:
    - 3 días: `[mod, pes, liv]`
    - 4 días: `[mod, pes, liv, mod]`
    - 5 días: `[mod, pes, liv, mod, liv]`
  - `Plantillas.escalonAbajo(CeldaPlantilla c, Tipo t) -> CeldaPlantilla`:
    - FUERZA: reps + 1 y −5 en `pctMin` y en `pctMax`;
    - HIPERTROFIA: reps + 2.
  - `Plantillas.obtener(Metodologia m, Tipo t, int semanas, int diasPorSemana) -> Plantilla`: busca en `m.plantillas()`. Si no existe, lanza `IllegalArgumentException`.
  - `DatosIniciales.metodologia()` incluye, para FUERZA e HIPERTROFIA, semanas 2–5 y días 3–5, las plantillas inferidas.

- [ ] **Paso 1: tests que fallan**

```java
@Test void fuerza3Dias() { var p = Plantillas.obtener(M, Tipo.FUERZA, 3, 3);
    assertEquals(List.of("3x5-75/80%","3x3-90%","3x6-70/75%"), p.semanas().get(0).stream().map(CeldaPlantilla::texto).toList());
    assertTrue(p.inferida()); }
@Test void hipertrofia3DiasSemana2() { assertEquals(List.of("4x10","3x8","4x12"),
    Plantillas.obtener(M, Tipo.HIPERTROFIA, 2, 3).semanas().get(1).stream().map(CeldaPlantilla::texto).toList()); }
@Test void cincoDiasUnSoloPesado() { var sem = Plantillas.obtener(M, Tipo.FUERZA, 4, 5).semanas().get(0);
    assertEquals(5, sem.size()); assertEquals(1, sem.stream().filter(c -> c.equals(sem.get(1))).count()); }
@Test void unDiaNoEsInferida() { assertFalse(Plantillas.obtener(M, Tipo.HIPERTROFIA, 5, 1).inferida()); }
```

- [ ] **Paso 2–4:** `mvn test -Dtest=PlantillasTest`: FAIL → implementar → PASS.
- [ ] **Paso 5:** commit `feat: plantillas básicas e inferencia para 3+ días`.

### Tarea 7: Ejercicios y pliometría (spec §6.5)

**Files:**
- Create: `src/main/java/com/rutina/model/{Ejercicio,Perfil,PuntoRm,PlanPliometria}.java`
- Create: `src/main/java/com/rutina/service/calculo/Pliometria.java`
- Modify: `DatosIniciales` (agregar `ejercicios()`)
- Test: `src/test/java/com/rutina/service/calculo/PliometriaTest.java`, `DatosInicialesTest` (agregar casos)

**Interfaces:**
- Produces:
  - `record Ejercicio(long id, long perfilId, String nombre, Tipo tipo, Double rm, Double rmObjetivo, int techoPct, Map<Zona,Integer> repsPorSerie, String grupoPlio, int orden, boolean activo)`
    - `repsPorSerie` tiene solo las zonas que el usuario pisó; las demás heredan del tipo.
    - `grupoPlio` es, por ejemplo, `"1A"` (solo pliometría; null en los demás).
    - `techoPct` es 100 por defecto.
  - Método `int repsPorSerie(Zona z, Metodologia m)` de `Ejercicio`: devuelve el valor propio si existe; si no, el del tipo.
  - `record Perfil(long id, String nombre)`, `record PuntoRm(LocalDate fecha, double rm)`, `record PlanPliometria(long id, long perfilId, int nivel, LocalDate inicio)`
  - `record DiaPlio(int contactos, List<String> ejercicios, int series)`: 10 saltos por serie, fijo.
  - `Pliometria.generar(Metodologia m, int nivel, List<Ejercicio> ejerciciosPlio) -> List<List<DiaPlio>>` (`[semana 0..3][día]`)
    - Cada día d (desde 1) usa el grupo `nivel + (char)('A'+d-1)`, ordenado por `orden`.
    - Toma exactamente 6 ejercicios: si el grupo tiene más, los 6 primeros; si tiene menos, vuelve a empezar la lista; si está vacío, una lista vacía.
    - `series = halfUp(contactos / 60.0)`.
  - `DatosIniciales.ejercicios() -> List<Ejercicio>`:
    - los 32 de la hoja Ejercicios, con 1RM y objetivo (spec §11.6), y tipo asignado por nombre según §11.6;
    - los 18 de la hoja Pliometria (1A, 1B y 1C, 6 cada uno), como PLIOMETRIA;
    - `id = 0` y `perfilId = 0` (se asignan al insertar).

- [ ] **Paso 1: tests que fallan**

```java
@Test void nivel1Semana1() { var s = Pliometria.generar(M, 1, plio("1A",6,"1B",6,"1C",6)).get(0);
    assertEquals(List.of(250,300,250), s.stream().map(DiaPlio::contactos).toList());
    assertEquals(List.of(4,5,4), s.stream().map(DiaPlio::series).toList()); }
@Test void nivel1Semana4() { assertEquals(List.of(6,5,6),
    Pliometria.generar(M,1,plio("1A",6,"1B",6,"1C",6)).get(3).stream().map(DiaPlio::series).toList()); }
@Test void listaCortaSeRepite() { assertEquals(List.of("a","b","c","d","a","b"),
    Pliometria.generar(M,1,plioNombres("1A","a","b","c","d")).get(0).get(0).ejercicios()); }
@Test void nivel3TieneDosDias() { assertEquals(2, Pliometria.generar(M,3,List.of()).get(0).size()); }
@Test void ejerciciosIniciales() { var e = DatosIniciales.ejercicios();
    assertEquals(32, e.stream().filter(x -> x.tipo() != Tipo.PLIOMETRIA).count());
    assertEquals(115.0, e.stream().filter(x -> x.nombre().equals("Cargada de potencia colgado")).findFirst().get().rm()); }
```
(`plio(...)` y `plioNombres(...)` son helpers del test que arman listas de `Ejercicio` PLIOMETRIA con ese `grupoPlio`.)

- [ ] **Paso 2–4:** `mvn test -Dtest='PliometriaTest,DatosInicialesTest'`: FAIL → implementar → PASS.
- [ ] **Paso 5:** commit `feat: ejercicios y cálculo de pliometría`.

### Tarea 8: Calculadora del mesociclo (orquestación, contadores y gráfico)

**Files:**
- Create: `src/main/java/com/rutina/model/{Mesociclo,Bloque,Aproximacion}.java`
- Create: `src/main/java/com/rutina/service/calculo/{CalculadoraMesociclo,ResultadoMesociclo}.java`
- Test: `src/test/java/com/rutina/service/calculo/CalculadoraMesocicloTest.java`

**Interfaces:**
- Consumes: todo lo de las Tareas 3–7.
- Produces:
  - `enum Aproximacion { COMPLEJA, BASICA }`
  - `record Bloque(int dia, int posicion, long ejercicioId, double pctBolsa)`
  - `record Mesociclo(long id, long perfilId, LocalDate inicio, int microciclos, ParametrosTipo potencia, ParametrosTipo fuerza, Character variante, Integer dias, Map<Long,Aproximacion> aproximaciones, List<Bloque> bloques, String calentamiento, String auxiliares, Map<Long,Double> rmUsados)`
    - `variante` y `dias` son null hasta que el usuario los elige.
    - `rmUsados` está vacío hasta el primer export.
  - `record SemanaBloque(int volumen, Map<Zona,List<List<Integer>>> seriesPorZona, CeldaPlantilla celda)`
    - Complejo: `seriesPorZona`, donde cada zona tiene **una lista por cada aparición del ejercicio en la semana** (en el orden de los días), y `celda` es null.
    - Básico, hipertrofia o pliometría: `celda` y el mapa vacío.
  - `record ResultadoBloque(Bloque bloque, Ejercicio ejercicio, Aproximacion aprox, List<SemanaBloque> semanas)`
  - `record Contador(Tipo tipo, int microciclo, int dia, int planificado, int asignado, int sinAsignar)`
  - `record PuntoGrafico(Tipo tipo, int microciclo, int volumen, double imr)`
  - `record ResultadoMesociclo(List<ResultadoBloque> bloques, List<Contador> contadores, List<PuntoGrafico> planificado, List<PuntoGrafico> real, List<String> avisos)`
  - `CalculadoraMesociclo.calcular(Metodologia m, Mesociclo mes, Map<Long,Ejercicio> ejercicios) -> ResultadoMesociclo` lanza `ParametrosInvalidosException`:
    - si falta la variante o los días;
    - si la combinación no existe;
    - si un IMR queda fuera de la tabla.

    Por cada tipo complejo (POTENCIA, y FUERZA con aproximación COMPLEJA):
    1. `porMicrociclo`;
    2. `pesosDias` con los días que tienen al menos un bloque complejo de ese tipo;
    3. `repsPorZona` → `repartirEnDias` → `repartir` (techo) → `GeneradorSeries.generar`. El arrastre corre **por ejercicio y zona a lo largo de los días del microciclo**.

    Básicos e hipertrofia usan `Plantillas.obtener(tipo, microciclos, apariciones por semana del ejercicio)`.
  - `avisos`: aviso en español cuando los `pctBolsa` de un tipo en un día no suman 100.
  - `real.imr` = Σ(reps × pct) / Σ reps de las series generadas. Si no hay reps, 0.

- [ ] **Paso 1: tests que fallan.** Fixture: 2A, 3 días, potencia `(0,72)`, fuerza `(200,70)`. Un solo bloque: día 1, ejercicio 1, FUERZA COMPLEJA, RM 100, techo 100, reps/serie por defecto.

```java
@Test void microciclo1() throws Exception { var r = calc();
    var sem = r.bloques().get(0).semanas().get(0);
    assertEquals(List.of(List.of(3,3,3,3,3,3)), sem.seriesPorZona().get(Zona.Z80));
    assertEquals(List.of(List.of(4,4)), sem.seriesPorZona().get(Zona.Z55));
    assertEquals(80, r.planificado().stream().filter(p -> p.tipo()==Tipo.FUERZA && p.microciclo()==1).findFirst().get().volumen());
    assertEquals(86, r.real().stream().filter(p -> p.tipo()==Tipo.FUERZA && p.microciclo()==1).findFirst().get().volumen()); }
@Test void contadorDiaUnico() throws Exception { var c = calc().contadores().stream()
        .filter(x -> x.tipo()==Tipo.FUERZA && x.microciclo()==1 && x.dia()==1).findFirst().get();
    assertEquals(80, c.planificado()); assertEquals(0, c.sinAsignar()); }
@Test void tipoSinEjercicios() throws Exception { // potencia con volumen pero sin bloques
    var r = calcConPotencia(new ParametrosTipo(100,72));
    assertEquals(40, r.contadores().stream().filter(x -> x.tipo()==Tipo.POTENCIA && x.microciclo()==1)
        .mapToInt(Contador::sinAsignar).sum()); }
@Test void faltaVariante() { assertThrows(ParametrosInvalidosException.class, () -> calcSinVariante()); }
@Test void ejercicioSinRmNoRompe() throws Exception { assertNotNull(calcConRm(null).bloques().get(0).semanas()); }
@Test void avisoPctBolsa() throws Exception { assertFalse(calcConDosBloques(60, 30).avisos().isEmpty()); }
```
Valores esperados: micro 1 = 80 reps a IMR 75. Por zona: `[6,6,10,9,10,18,14,5,2,0]`. Series reales: 8+8+12+8+9+18+15+6+2 = 86. Para potencia sin bloques, con 100 de volumen, micro 1 = 40 reps sin asignar.

- [ ] **Paso 2–4:** `mvn test -Dtest=CalculadoraMesocicloTest`: FAIL → implementar → PASS.
- [ ] **Paso 5:** commit `feat: calculadora del mesociclo con contadores`.

### Tarea 9: Exportador PDF (spec §7)

**Files:**
- Create: `src/main/java/com/rutina/pdf/ExportadorPdf.java`
- Modify: `pom.xml` (OpenPDF)
- Test: `src/test/java/com/rutina/pdf/ExportadorPdfTest.java`

**Interfaces:**
- Consumes: `ResultadoMesociclo`, `Pliometria.generar`, `Redondeo.kg`.
- Produces: `ExportadorPdf.exportar(Path destino, String perfil, Mesociclo mes, ResultadoMesociclo res, List<PlanConDias> planes, Metodologia m) -> void`, con `record PlanConDias(PlanPliometria plan, List<List<DiaPlio>> semanas, Set<Integer> semanasDentroDelMesociclo)`.
  - A4 apaisado; el layout es el de spec §7.
  - Filas de kg: 55, 60, 65, 70, 75, `80–87,5`, `90–95` y 100.
  - Rango con guion simple: `"80-88 kg"`. Si no hay RM, la etiqueta es solo el % (`"80-87,5%"`).
  - Fila agrupada = concatenación con punto de la notación de cada zona.
  - Página de pliometría solo si `planes` no está vacío. Cada día lista sus 6 ejercicios con la notación `"<series> x 10"` (por ejemplo, `"4 x 10"`), y las semanas fuera del mesociclo van en gris.

- [ ] **Paso 1: tests que fallan** (texto extraído con `PdfTextExtractor` de OpenPDF)

```java
@Test void unaPaginaSinPlio() { exportar(List.of()); assertEquals(1, paginas()); assertTrue(texto(1).contains("80-88 kg")); }
@Test void filaAgrupadaConcatena() { exportar(List.of()); assertTrue(texto(1).contains("3.3.3.3.3.3")); }
@Test void paginaPliometria() { exportar(List.of(planNivel1())); assertEquals(2, paginas()); assertTrue(texto(2).contains("4 x 10")); }
@Test void sinRmUsaPorcentaje() { exportarSinRm(); assertTrue(texto(1).contains("80-87,5%")); }
```
(Fixture: el mismo de la Tarea 8.)

- [ ] **Paso 2–4:** `mvn test -Dtest=ExportadorPdfTest`: FAIL → implementar → PASS. Además, abrir el PDF generado en `target/` y compararlo a ojo con `Ejemplo de rutina.jpg`.
- [ ] **Paso 5:** commit `feat: exportación del mesociclo a PDF`.

### Tarea 10: Persistencia SQLite

**Files:**
- Create: `src/main/java/com/rutina/service/datos/{Database,PerfilRepo,EjercicioRepo,MetodologiaRepo,MesocicloRepo,PlanPlioRepo}.java`
- Test: `src/test/java/com/rutina/service/datos/PersistenciaTest.java` (con `@TempDir`)

**Interfaces:**
- Produces:
  - `Database.rutaPorDefecto() -> Path`
  - `Database.abrir(Path archivo) -> Database`:
    - crea el esquema;
    - si la base es nueva, guarda `DatosIniciales.metodologia()` como JSON (Gson) en la tabla `metodologia` (id = 1);
    - si el archivo es ilegible, lo renombra a `rutina.db.bak-<yyyyMMddHHmmss>`, crea uno nuevo y deja `boolean seRestauro()` en true.
  - `Connection conexion()`
  - `PerfilRepo`:
    - `listar()`;
    - `crear(String nombre) -> Perfil`: inserta `DatosIniciales.ejercicios()`. El primer perfil los recibe con sus 1RM; los siguientes, sin 1RM.
  - `EjercicioRepo`:
    - `listar(long perfilId)`;
    - `guardar(Ejercicio) -> Ejercicio` (inserta si `id == 0`);
    - `actualizarRm(long id, double rm, LocalDate fecha)`: guarda el valor anterior en el historial;
    - `historial(long id) -> List<PuntoRm>`.
  - `MetodologiaRepo`: `cargar()`, `guardar(Metodologia)`, `restaurar()` (vuelve a `DatosIniciales`).
  - `MesocicloRepo`: `listar(long perfilId)`, `guardar(Mesociclo) -> Mesociclo`, `borrar(long id)`.
  - `PlanPlioRepo`: `listar(long perfilId)`, `guardar(PlanPliometria) -> PlanPliometria`, `borrar(long id)`.

  Tablas: `perfil`, `ejercicio`, `historial_rm`, `metodologia`, `mesociclo` (bloques, aproximaciones y `rmUsados` en columnas JSON), `plan_pliometria`.

- [ ] **Paso 1: tests que fallan**

```java
@Test void baseNuevaTraeMetodologia(@TempDir Path d) { var db = Database.abrir(d.resolve("r.db"));
    assertEquals(10, new MetodologiaRepo(db).cargar().variantes().size()); }
@Test void historialRm(@TempDir Path d) { var db = Database.abrir(d.resolve("r.db")); var p = new PerfilRepo(db).crear("Franco");
    var repo = new EjercicioRepo(db); var e = repo.listar(p.id()).get(0);
    repo.actualizarRm(e.id(), 120, LocalDate.of(2026,11,1));
    assertEquals(2, repo.historial(e.id()).size()); assertEquals(120.0, repo.listar(p.id()).get(0).rm()); }
@Test void segundoPerfilSinRm(@TempDir Path d) { var db = Database.abrir(d.resolve("r.db")); var pr = new PerfilRepo(db);
    pr.crear("Franco"); var amigo = pr.crear("Amigo");
    assertTrue(new EjercicioRepo(db).listar(amigo.id()).stream().allMatch(x -> x.rm() == null)); }
@Test void mesocicloIdaYVuelta(@TempDir Path d) { /* guardar un Mesociclo con 2 bloques y aproximación; listar; assertEquals */ }
@Test void archivoCorruptoSeRespalda(@TempDir Path d) throws Exception { var f = d.resolve("r.db");
    Files.writeString(f, "basura"); var db = Database.abrir(f);
    assertTrue(db.seRestauro()); assertEquals(1, Files.list(d).filter(x -> x.getFileName().toString().startsWith("r.db.bak-")).count()); }
```

- [ ] **Paso 2–4:** `mvn test -Dtest=PersistenciaTest`: FAIL → implementar → PASS.
- [ ] **Paso 5:** commit `feat: persistencia SQLite con perfiles e historial de RM`.

### Tarea 11: Ventana principal, perfiles y pestaña Ejercicios

**Files:**
- Create: `src/main/java/com/rutina/Main.java`, `src/main/java/com/rutina/ui/{VentanaPrincipal,SelectorPerfil,PanelEjercicios,FichaEjercicio,Graficos}.java`
- Modify: `pom.xml` (JFreeChart)

**Interfaces:**
- Produces:
  - `Graficos.evolucionRm(List<PuntoRm>, Double objetivo) -> JPanel`
  - `Graficos.volumenIntensidad(List<PuntoGrafico> plan, List<PuntoGrafico> real) -> JPanel`
  - Los usan las Tareas 13 y 14.

Pasos:
- [ ] `Main`: FlatLaf, abre la base con `Database.abrir(rutaPorDefecto())`, avisa si `seRestauro()` y muestra `SelectorPerfil`. Si hay un solo perfil, entra directo; si no hay ninguno, pide un nombre.
- [ ] `VentanaPrincipal`: pestañas Ejercicios, Plantillas, Macrociclo y Mesociclo.
- [ ] `PanelEjercicios`: cuatro secciones (Potencia, Fuerza, Hipertrofia y Pliometría).
  - Pliometría muestra los grupos 1A…3B como listas reordenables.
  - Agregar, renombrar y desactivar.
- [ ] `FichaEjercicio` (diálogo):
  - 1RM, objetivo y techo;
  - tabla de reps/serie por zona, donde lo heredado aparece en gris y lo propio en negro;
  - `Graficos.evolucionRm`.
- [ ] `mvn package`, después `java -jar target/*-jar-with-dependencies.jar`.
- [ ] Prueba manual:
  - crear perfil;
  - cambiar el 1RM de la cargada y ver 2 puntos en el gráfico;
  - pisar las reps del 80 % y ver que el resto sigue heredado;
  - crear un segundo perfil y comprobar que no tiene 1RM.
- [ ] Commit `feat: ventana, perfiles y pestaña Ejercicios`.

### Tarea 12: Pestaña Plantillas

**Files:**
- Create: `src/main/java/com/rutina/ui/PanelPlantillas.java`

Pasos:
- [ ] Siete secciones editables (spec §5.2), cada una como `JTable` sobre `Metodologia`.
  - Las plantillas se eligen con tipo / semanas / días; si son inferidas, muestran la etiqueta "inferida".
  - Al editar una celda de una plantilla inferida, la plantilla pasa a `inferida = false`.
- [ ] Campo "Redondeo de kg" (1 / 2,5), que edita `incrementoKg`.
- [ ] Botones "Guardar" (`MetodologiaRepo.guardar`) y "Restaurar valores originales" (con confirmación).
- [ ] Validar al guardar: las celdas de plantilla se interpretan con `CeldaPlantilla.parse`; si una no se puede interpretar, se marca en rojo y no se guarda.
- [ ] Prueba manual:
  - editar una celda de 4×3 de fuerza: la etiqueta "inferida" desaparece;
  - restaurar: vuelve.
- [ ] Commit `feat: pestaña Plantillas`.

### Tarea 13: Pestaña Macrociclo

**Files:**
- Create: `src/main/java/com/rutina/ui/PanelMacrociclo.java`

Pasos:
- [ ] Calendario del año elegido, en filas de semanas desde el lunes y con los meses. Los mesociclos y los planes de pliometría se muestran como franjas.
- [ ] "Nuevo mesociclo":
  - pide inicio (lunes), microciclos (2–5) y volumen + %IMR de potencia y de fuerza;
  - valida los IMR contra el rango que admite alguna variante para esa cantidad de microciclos. Usa `DistribucionVolumen.porMicrociclo` con cada variante y avisa si ninguna sirve.
- [ ] "Nuevo plan de pliometría": nivel e inicio; muestra los solapamientos.
- [ ] Gráfico anual con `Graficos.volumenIntensidad` (planificado; real si el mesociclo ya está armado).
- [ ] Doble click en un mesociclo abre la pestaña Mesociclo con ese mesociclo.
- [ ] Prueba manual: crear 2 mesociclos seguidos y un plan de pliometría que los cruce; verlos en el calendario y en el gráfico.
- [ ] Commit `feat: pestaña Macrociclo`.

### Tarea 14: Pestaña Mesociclo (armado y exportación)

**Files:**
- Create: `src/main/java/com/rutina/ui/{PanelMesociclo,DialogoElegirEjercicio}.java`

Pasos:
- [ ] Selectores de variante (solo las que existen para esa cantidad de microciclos) y de días (3/4/5).
- [ ] Grilla: Día 1…N como columnas y bloques E1, E2… como filas, con "+" para agregar bloques.
  - Click en un bloque → `DialogoElegirEjercicio`: lista agrupada por tipo, solo ejercicios activos.
  - Para un ejercicio de FUERZA que todavía no está en `aproximaciones`, preguntar "Compleja / Básica".
- [ ] Campo `% de la bolsa` en cada bloque complejo. Por defecto, partes iguales entre los del mismo tipo y día.
- [ ] Recalcular con `CalculadoraMesociclo.calcular` en cada cambio:
  - series de solo lectura en cada bloque;
  - panel de contadores (planificado / asignado / sin asignar por tipo, microciclo y día);
  - `avisos` y errores de `ParametrosInvalidosException` en una barra visible.
- [ ] Calentamiento y auxiliares (texto libre); `Graficos.volumenIntensidad`.
- [ ] "Exportar PDF":
  - guarda `rmUsados`;
  - arma los `PlanConDias` de los planes que se solapan;
  - llama a `ExportadorPdf.exportar` (destino con `JFileChooser`; por defecto `~/Documentos/`);
  - ofrece "Recalcular con 1RM actuales".
- [ ] Prueba manual:
  - armar el mesociclo actual del usuario (5A, 3 días; potencia 250 @ 72, fuerza 350 @ 76);
  - poner dos ejercicios de fuerza el lunes, uno con techo 85;
  - verificar que el día 2 sin fuerza reparte su volumen entre los otros días;
  - exportar y comparar con la planilla del entrenador.
- [ ] Commit `feat: armado del mesociclo y exportación`.

### Tarea 15: README y acceso directo

**Files:**
- Create: `README.md`

Pasos:
- [ ] README en español:
  - qué hace;
  - requisitos (Java 21);
  - `mvn package`, `java -jar target/rutina-1.0.0-jar-with-dependencies.jar`, `mvn test`;
  - **dónde guarda los datos** (`~/.local/share/rutina/rutina.db`);
  - diagrama Mermaid del flujo: macrociclo → mesociclo → cálculo → PDF.
- [ ] `mvn test` completo en verde.
- [ ] Commit `docs: README`.
- [ ] Preguntarle al usuario si quiere el acceso directo `.desktop` en `~/.local/share/applications/` y en qué carpeta del escritorio, y sugerir `/init`.
