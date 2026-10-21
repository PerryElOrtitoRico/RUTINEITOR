# Generador de rutinas — Documento de diseño

*Fecha: 2026-10-21 · Estado: borrador para aprobación*

Fuentes: `Rutina.xlsx` (lógica actual), `Aproximacion hip.-Fza.Basica.xlsx` (plantillas), `Ejemplo de rutina.jpg` (planilla del entrenador) y el libro *Actualizaciones sobre el entrenamiento de la potencia* de H. Anselmi (`Libros/PLANIFICACION DEL ENTRENAMIENTO.pdf`): cap. 6 (pliometría, págs. 93–99) y cap. 8 (método cubano, págs. 132–135).

Las decisiones marcadas **[decisión de diseño]** las tomó quien escribe el documento, no el usuario. Se pueden revisar.

---

## 1. Para qué es

El usuario programa sus mesociclos de sobrecarga (halterofilia y ejercicios auxiliares) con un Excel propio que implementa el método cubano de distribución de repeticiones. Los cálculos teóricos son correctos, pero **traducirlos a sesiones entrenables le lleva mucho tiempo y le genera dudas**:

- reparte a mano las repeticiones de cada zona de intensidad y queda con sesiones incómodas (10 repeticiones con un peso, después 2 con el siguiente, después 7 con otro…), sin una progresión cómoda dentro del día;
- no tiene la certeza de haber respetado el volumen y la intensidad de cada semana;
- el Excel admite un solo ejercicio de potencia y uno de fuerza por día, y las casillas no saben de qué tipo es cada ejercicio;
- para armar un mesociclo tiene que acordarse de cómo había planificado el año.

**Éxito:** armar un mesociclo en minutos, con series que se entrenen cómodas (escalera de peso ascendente con repeticiones por serie razonables), con confianza en que respeta la teoría, y salir con un PDF para llevar al gimnasio.

## 2. Usuarios

- El usuario, en su PC (Linux). Uso principal.
- Ocasionalmente un amigo que usa la misma PC para armarse su rutina. Por eso: **perfiles separados**, y la parte "de entrenador" (tablas y plantillas) tiene que quedar **aparte de lo cotidiano**, para que alguien que no sabe no se pierda ni la rompa.

## 3. Objetivos, no-objetivos y anti-objetivos

**Objetivos**
1. Generar automáticamente series × repeticiones × kg de cada ejercicio, día y microciclo a partir de los parámetros del mesociclo.
2. Series cómodas: repeticiones por serie fijas por zona de intensidad (configurables por ejercicio) y escalera ascendente de peso.
3. Varios ejercicios por día de cualquier tipo (potencia, fuerza, hipertrofia, pliometría), con el volumen de cada tipo como una "bolsa" que se reparte.
4. Planificar el año (macrociclo) y armar cada mesociclo a partir de lo planificado.
5. Exportar un PDF con el formato de la planilla del entrenador.
6. Gráficos: volumen e intensidad reales, evolución del 1RM y vista anual.

**No-objetivos**
- App para celular, sincronización o nube. En el gimnasio se usa solo el PDF.
- Registrar lo que realmente se entrenó día a día (bitácora). Solo se guarda el historial de 1RM.
- Varias personas usando el programa al mismo tiempo, o en red.

**Anti-objetivos** (cosas que, aunque funcionen, serían un fracaso)
- Que el programa invente su propia teoría. Los números salen de las tablas del método (cap. 8) y de las plantillas del usuario, nada más.
- Que el usuario tenga que seguir repartiendo repeticiones a mano para que "cierre".
- Que las tablas de metodología estén mezcladas con la lista de ejercicios.
- Que un cambio de 1RM pise el valor anterior sin dejar historial.

## 4. Cómo se construye

Aplicación de escritorio en **Java 21 + Maven**, interfaz **Swing + FlatLaf**, datos en **SQLite** local (`sqlite-jdbc`), tests con **JUnit 5**, distribuida como `jar-with-dependencies`. Son las convenciones del usuario (`Proyectos/CLAUDE.md`). **[decisión de diseño]** PDF con **OpenPDF** y gráficos con **JFreeChart**, porque Swing no trae ni PDF ni gráficos.

La base de datos va en un único archivo en `$XDG_DATA_HOME/rutina/` (o `~/.local/share/rutina/`), nunca en el directorio actual. **[decisión de diseño]** Al crearla por primera vez se carga con los datos de `Rutina.xlsx` y del Excel de plantillas (sección 11), y esos datos van embebidos en el código o en un recurso: el programa **no lee los Excel en tiempo de ejecución**.

## 5. Pantallas (pestañas)

Al abrir el programa se elige o se crea un **perfil**. Si hay uno solo, entra directamente. Cada perfil tiene sus ejercicios, 1RM con historial, macrociclo, mesociclos y planes de pliometría. Las **plantillas y tablas del método son compartidas** entre todos los perfiles.

### 5.1 Ejercicios

Cuatro secciones: **Potencia, Fuerza, Hipertrofia y Pliometría**.

- **Potencia, Fuerza e Hipertrofia**: lista de ejercicios. Al hacer click se abre la **ficha** del ejercicio, con:
  - nombre, tipo, activo/inactivo;
  - **1RM actual** y **1RM objetivo**. Cada cambio de 1RM guarda el valor anterior con su fecha;
  - **gráfico de evolución del 1RM**;
  - **techo de intensidad** (solo potencia y fuerza): la zona más alta que puede recibir este ejercicio (por defecto 100 %; por ejemplo, la cargada de colgado hasta 85 %). **[decisión de diseño]** Es una propiedad del ejercicio, no del mesociclo;
  - **repeticiones por serie por zona de intensidad** (55 … 100 %) de este ejercicio, solo para potencia y fuerza. Por defecto hereda los valores del tipo (pestaña Plantillas); el usuario puede pisar zonas sueltas.
  - Se pueden agregar, renombrar y desactivar ejercicios. Desactivar en lugar de borrar, para no romper mesociclos viejos.
- **Pliometría**: listas de ejercicios por **nivel y día** (Nivel 1 A/B/C, Nivel 2 A/B/C, Nivel 3 A/B), editables. Los ejercicios de pliometría no tienen 1RM.

Un ejercicio sin 1RM cargado (por ejemplo, auxiliares con peso corporal) se puede usar igual: en el PDF el kg queda vacío.

### 5.2 Plantillas (metodología)

Pestaña separada, pensada para el usuario "entrenador". Todo es editable y tiene un botón **restaurar valores originales**.

1. **Distribución del volumen por microciclo** (variantes 5A, 5B, 4A, 4B, 4C, 3A, 3B, 3C, 2A, 2B): % de volumen y ΔIMR de cada microciclo.
2. **Distribución del volumen por día** (3, 4 y 5 días por microciclo).
3. **Tabla de distribución de repeticiones por intensidad** (IMR 60–80 % → % de repeticiones en cada zona, de 55 a 100 %).
4. **Repeticiones por serie por zona, por defecto de cada tipo** (potencia y fuerza).
5. **Plantillas de fuerza básica**: semanas (2–5) × días por semana del ejercicio.
6. **Plantillas de hipertrofia**: ídem.
7. **Volúmenes de pliometría** por nivel, semana y día.

Las plantillas que el programa haya inferido (sección 6.4) se muestran **marcadas como "inferida"** hasta que el usuario las edite o las confirme.

### 5.3 Macrociclo (año)

- Calendario del año por **semanas que empiezan en lunes**, con los meses (como la hoja Programacion).
- El usuario encadena **mesociclos** sobre el calendario. Cada uno lleva: fecha de inicio, **cantidad de microciclos (2–5)**, y **volumen total (repeticiones) y %IMR de potencia y de fuerza**, decididos de antemano. Cada microciclo equivale a una semana.
- En el mismo calendario se ubican los **planes de pliometría** (nivel y fecha de inicio; duran 4 semanas) y se ven sus solapamientos con los mesociclos.
- **Gráfico anual**: volumen e intensidad planificados de cada mesociclo y, cuando el mesociclo ya está armado, los reales.
- Desde el calendario se abre un mesociclo para armarlo (5.4) con los parámetros ya cargados.

### 5.4 Armado del mesociclo

Parámetros que se eligen al armar: **variante (A/B/C)** y **días por microciclo (3, 4 o 5)**. Si la combinación de cantidad de microciclos y variante no existe (por ejemplo 5C o 2C), se avisa y no se deja seguir.

**Grilla**: una columna por **día del microciclo** (Día 1 … Día N) y, en cada día, bloques de ejercicio (E1, E2, E3…), con la cantidad que haga falta. La estructura de días y ejercicios es la misma para todos los microciclos: cambian las series. Esto replica la planilla del entrenador.

- **Click en un bloque** → lista de ejercicios agrupada por tipo → se elige uno.
- Un ejercicio de **fuerza**, la primera vez que aparece en el mesociclo, pregunta **"aproximación compleja o básica"**. La elección vale para ese ejercicio en todo el mesociclo (en todos los días donde aparezca) y se puede cambiar después. Potencia siempre es compleja; hipertrofia siempre usa plantilla.
- **Bolsa por tipo**: el volumen de potencia y el de fuerza de cada día y microciclo (sección 6.1) se reparte entre los ejercicios **complejos** de ese tipo en ese día. Si hay más de uno, el usuario indica el **% de cada uno**. **[decisión de diseño]** Arranca en partes iguales y avisa si la suma no da 100 %. Los ejercicios básicos y de hipertrofia **no** descuentan de la bolsa.
- **Contadores en vivo** por tipo, día y microciclo: repeticiones planificadas, asignadas y diferencia, como los contadores actuales del Excel pero automáticos.
- Las series de cada ejercicio se generan solas (sección 6) y se ven en la grilla en modo solo lectura.
- **Calentamiento** y **auxiliares**: texto libre del mesociclo.
- **Gráfico del mesociclo**: volumen e intensidad **planificados vs reales** por microciclo.
- Botón **Exportar PDF** (sección 7).

**[decisión de diseño]** Al exportar, el mesociclo guarda una **foto de los 1RM** que se usaron, para poder reimprimir el mismo PDF aunque después cambien los 1RM. También se ofrece "recalcular con los 1RM actuales".

## 6. Lógica de cálculo

### 6.1 Volumen e intensidad por microciclo y por día (aproximación compleja)

Para cada tipo T ∈ {potencia, fuerza}, con volumen total `V_T` e intensidad `I_T` del mesociclo, y variante `nV` (por ejemplo, "4A"):

- Volumen del microciclo m: `V_T,m = V_T × volPct[nV][m] / 100`
- IMR del microciclo m: `I_T,m = I_T + ΔIMR[nV][m]`
- Volumen del día d: `V_T,m,d = V_T,m × diaPct[días][d]`

Es la misma lógica que la hoja Entrenamiento (fila 5 y columnas A–E) del Excel.

**Días sin ese tipo (decidido por el usuario):** el volumen de un tipo se reparte solo entre los días del microciclo que tienen al menos un ejercicio complejo de ese tipo. Los % de `diaPct` de esos días se reescalan para sumar 100 %. Si el tipo aparece en un solo día, ese día se lleva todo el volumen del microciclo. Ejemplo: con 4 días y fuerza solo en D1 (26 %) y D3 (28 %), quedan D1 = 26/54 y D3 = 28/54.

**Validación:** `I_T,m` tiene que caer dentro de la tabla de distribución (60–80 %). Si no cae, se avisa al cargar los parámetros (el Excel tiene un comentario sobre esto).

### 6.2 Repeticiones por zona (método cubano, cap. 8)

- Repeticiones del microciclo en la zona z: `R_T,m,z = V_T,m × tablaRef[I_T,m][z]`.
- **[decisión de diseño]** Cada fila de la tabla se **normaliza para que sume 100 %**, porque la tabla original tiene filas que no suman 100 (ver 11.3).
- Reparto a los días: `R_T,m,d,z = R_T,m,z × diaPct[d]`, redondeando con el método del **mayor resto**, para que la suma de los días dé exactamente el total de la zona.
- Si hay varios ejercicios complejos del mismo tipo en el día, cada uno se lleva su % de cada `R_T,m,d,z` (también con mayor resto).
- **Techo de intensidad (decidido por el usuario):** un ejercicio no recibe repeticiones en zonas por encima de su techo. Las repeticiones de esas zonas **pasan a los otros ejercicios del mismo tipo de ese día** que sí admiten la zona, en proporción a sus %. **[decisión de diseño]** Si ningún ejercicio del día admite la zona, esas repeticiones quedan sin asignar y se muestran como faltante en los contadores y en el gráfico.

### 6.3 De repeticiones a series (el corazón del programa)

Para un ejercicio, una zona z y un día, con `r` repeticiones y `s` = repeticiones por serie de esa zona para ese ejercicio:

- cantidad de series `n = round(r / s)`; si `n = 0` no se hace ninguna serie ese día y las `r` repeticiones pasan al día siguiente (arrastre). **[decisión de diseño, ajustada al implementar]** Así nunca aparecen series sueltas de otro tamaño (por ejemplo, una serie de 1 al 55 %);
- se anota como en el libro: `s.s.s` (por ejemplo, `3.3.3.3` = 4 series de 3);
- la diferencia entre `n × s` y `r` se suma a los contadores, así el usuario ve cuánto se apartó de la teoría. **[decisión de diseño]** Se acepta este pequeño desvío a cambio de series limpias.
- **Arrastre del resto [decisión de diseño, ajustada al implementar]:** en cada día, las zonas de un ejercicio se recorren de la más liviana a la más pesada y la diferencia `r − n × s` de cada zona **pasa a la siguiente zona que el ejercicio trabaja ese día** (una zona sin repeticiones planificadas, o por encima del techo, se saltea y nunca recibe series) antes de calcular sus series. El sobrante de la última zona queda como desvío. Así el volumen de cada día queda a menos de media serie de lo planificado (con arrastre solo entre días, un día con dos ejercicios de potencia al 50 % llegaba a +17 reps sobre 29). Redondeo de mitades: hacia arriba (`HALF_UP`).

Valores por defecto de `s` (editables en Plantillas), tomados del ejemplo del libro (pág. 136):

| Zona | 55 | 60 | 65 | 70 | 75 | 80 | 85 | 90 | 95 | 100 |
|---|---|---|---|---|---|---|---|---|---|---|
| reps/serie | 4 | 4 | 4 | 4 | 3 | 3 | 3 | 2 | 1 | 1 |

**[decisión de diseño]** Se usan los mismos valores por defecto para potencia y para fuerza. El usuario los ajusta por tipo o por ejercicio.

El día queda ordenado como **escalera ascendente** de zonas (de 55 % hacia arriba).

**kg** = 1RM × zona. **[decisión de diseño]** Se redondea a 1 kg, como el Excel. El incremento de redondeo (1 / 2,5 kg) es configurable.

### 6.4 Fuerza básica e hipertrofia (plantillas)

Se busca la plantilla del tipo para (cantidad de semanas del mesociclo, cantidad de días por semana en que aparece ese ejercicio). El ejercicio recibe, para cada aparición en orden (semana 1 día 1, semana 1 día 2, …), la celda que le corresponde.

- **Fuerza básica**: `series x reps - %` o `series x reps - %a/%b` → en el PDF va con kg calculados con el 1RM (rango si la plantilla tiene rango): `3x5 · 86–92 kg`.
- **Hipertrofia**: `series x reps`, sin kg. El peso lo elige el usuario en el momento.

**Plantillas faltantes (3, 4 o 5 días por semana)**: el programa las **infiere** con la misma ondulación y las marca como "inferida":

- **3 días**: ondulación diaria **moderado / pesado / liviano**. Fuerza: D1 como el "día 1" de 2 días, D2 como el "día 2" de 2 días y D3 un escalón por debajo del D1 (por ejemplo, `3x5-75/80%` · `3x3-90%` · `3x6-70/75%`). Hipertrofia: análogo (por ejemplo, `4x10` · `3x8` · `4x12`). Entre semanas, cada día sigue la ola que ya tiene la plantilla de 2 días.
- **4 o 5 días**: se repite la alternancia moderado/pesado, con **un solo día pesado de verdad por semana**; los demás son moderados o livianos.

Lo que el usuario confirmó: la ondulación entre semanas (sube por escalones con bajadas para recuperar y termina en el pico) y la regla para 3 o más días. El detalle de cada celda inferida lo decide el constructor siguiendo esa regla, y el usuario la revisa en Plantillas.

### 6.5 Pliometría

Un **plan de pliometría** es independiente del mesociclo: tiene nivel (1, 2 o 3), fecha de inicio y **siempre 4 semanas**, con 3 días (niveles 1 y 2) o 2 días (nivel 3). No se adapta a la duración ni a los días del mesociclo.

- Contactos del día: tabla de volúmenes del nivel (11.4).
- Ejercicios del día: la lista del nivel y día (Nivel 1 día 1 → "Nivel 1 A", día 2 → "1 B", día 3 → "1 C"). **Siempre 6 ejercicios.**
- Series por ejercicio: `round(contactos / 60)`, con **10 saltos por serie**. Ejemplos confirmados: 250 → 6 × 4 × 10; 300 → 6 × 5 × 10; 350 → 6 × 6 × 10.
- Si la lista del día tiene más o menos de 6 ejercicios, se toman los 6 primeros o se vuelve a empezar la lista. **[decisión de diseño]**
- Las anotaciones entre corchetes en algunos nombres (por ejemplo, `[2]`) se tratan como parte del texto del ejercicio. **[decisión de diseño]**

## 7. PDF

Un solo archivo, A4 **apaisado**. Toma como base la planilla del entrenador (`Ejemplo de rutina.jpg`).

**Página 1 — rutina de gimnasio**
- Encabezado: nombre del perfil, mesociclo y fechas; **calentamiento** (texto libre).
- Grilla: columnas = días del microciclo y, en cada día, bloques E1, E2, E3…
- Cada bloque es una tabla: título = ejercicio; **columnas = semanas (microciclos)**; fila superior = volumen de la semana para ese ejercicio.
- **Filas = zonas, mostradas como kg (no como %)**, calculados con el 1RM de ese ejercicio:
  - 55, 60, 65, 70 y 75 %: una fila cada una;
  - **80–87,5 %** y **90–95 %**: agrupadas, con el kg como rango (por ejemplo, `92–101 kg` con 1RM 115);
  - 100 %: fila propia.
  - El agrupamiento es **solo visual**: el cálculo sigue haciéndose de a 5 % y, en la fila agrupada, se concatenan las series de sus zonas (`3.3.2.2`).
- Celdas: notación de series (`3.3.3`).
- Ejercicios básicos y de hipertrofia: en lugar de filas por zona, una fila por semana con `3x5 · 86–92 kg` o `4x12`.
- Pie: **auxiliares** (texto libre).
- Si no entra en una página, se parte en varias páginas de la misma rutina. **[decisión de diseño]**

**Página 2 — pliometría**
- El plan de pliometría que se solapa con el mesociclo: nivel, sus 4 semanas × días, ejercicios y `series × 10`.
- **[decisión de diseño]** Si el plan empezó antes del mesociclo o termina después, se muestra el plan completo, con las semanas que caen dentro del mesociclo resaltadas. Si hay **más de un plan** que se solapa con el mesociclo (uno termina y empieza el siguiente), se muestran todos, uno debajo del otro (o en páginas sucesivas si no entran). Si no hay plan, no se incluye la página.

## 8. Gráficos

1. **Mesociclo**: por microciclo, volumen (repeticiones) e intensidad (IMR = promedio de las zonas ponderado por repeticiones), **planificado vs real**, donde "real" es lo que quedó después de redondear las series.
2. **Ejercicio**: evolución del 1RM en el tiempo, con el 1RM objetivo como línea de referencia.
3. **Año**: volumen e intensidad de cada mesociclo en el calendario.

## 9. Datos (orientativo)

Entidades: Perfil · Ejercicio (tipo, 1RM, objetivo, reps/serie por zona propias) · HistorialRM · Plantillas compartidas (tablas de 5.2) · Macrociclo/Mesociclo (fechas, microciclos, V e I de potencia y fuerza, variante, días, aproximación por ejercicio, foto de 1RM, calentamiento, auxiliares) · Bloque (día, posición, ejercicio, % de la bolsa) · PlanPliometría (nivel, inicio). Las series **no se guardan**: se recalculan. El esquema exacto queda a criterio del constructor.

## 10. Lo que queda a criterio del constructor

Estructura del código, esquema SQL, librerías de PDF y gráficos, diseño visual de las pantallas, atajos de teclado, cómo se ve el aviso de suma ≠ 100 %, empaquetado de JavaFX en el jar. La lógica de cálculo (sección 6) tiene que tener tests unitarios. Hay que contrastarla con los valores que hoy calcula el Excel del usuario: por ejemplo, 163 repeticiones con IMR 74 % → 13, 16, 18, 20, 21, 36, 29, 10 (hoja Ref, fila 26), y con las fórmulas de la hoja Entrenamiento. Los ejemplos del libro usan otra variante de microciclos y redondean distinto (el libro da 19 en la zona de 70 %), así que no sirven como valor esperado exacto. **Cuando el documento y el Excel no coinciden** (normalización de filas, filas 65/66 corregidas, mayor resto, arrastre del resto), **manda el documento**: los tests que comparan con el Excel se limitan a casos donde las dos reglas dan lo mismo, y el resto se testea con valores calculados a mano según la sección 6.

## 11. Datos iniciales (de los Excel del usuario)

### 11.1 Distribución por microciclo (Calculos)

| Var. | Vol% m1 | m2 | m3 | m4 | m5 | ΔIMR m1 | m2 | m3 | m4 | m5 |
|---|---|---|---|---|---|---|---|---|---|---|
| 5A | 21 | 23 | 26 | 18 | 12 | 0 | −1 | −2 | +2 | +3 |
| 5B | 20 | 22 | 25 | 18 | 15 | 0 | −1 | −2 | +1 | +3 |
| 4A | 28 | 22 | 32 | 18 | | 0 | +2 | −3 | +3 | |
| 4B | 22 | 28 | 32 | 18 | | +2 | 0 | −3 | +3 | |
| 4C | 24 | 26 | 28 | 22 | | +1 | −1 | −2 | +2 | |
| 3A | 20 | 30 | 50 | | | +4 | +2 | −3 | | |
| 3B | 25 | 35 | 40 | | | +3 | +1 | −2 | | |
| 3C | 20 | 35 | 45 | | | +4 | +2 | −3 | | |
| 2A | 40 | 60 | | | | +5 | −3 | | | |
| 2B | 45 | 55 | | | | +3 | −3 | | | |

### 11.2 Distribución por día (Calculos)

| Días | D1 | D2 | D3 | D4 | D5 |
|---|---|---|---|---|---|
| 3 | 35 % | 45 % | 20 % | | |
| 4 | 26 % | 24 % | 28 % | 22 % | |
| 5 | 20 % | 15 % | 30 % | 10 % | 25 % |

### 11.3 Distribución de repeticiones por intensidad (Ref, % de repeticiones por zona)

| IMR | 55 | 60 | 65 | 70 | 75 | 80 | 85 | 90 | 95 | 100 | suma |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 60 | 55 | 40 | 15 | 10 | | | | | | | 120 |
| 61 | 30 | 35 | 20 | 15 | | | | | | | 100 |
| 62 | 25 | 30 | 25 | 20 | | | | | | | 100 |
| 63 | 20 | 35 | 20 | 15 | 10 | | | | | | 100 |
| 64 | 15 | 25 | 35 | 15 | 10 | | | | | | 100 |
| 65 | 15 | 20 | 30 | 15 | 15 | | | | | | 95 |
| 66 | 14 | 18 | 30 | 18 | 15 | 8 | | | | | 103 |
| 67 | 10 | 20 | 25 | 20 | 15 | 10 | | | | | 100 |
| 68 | 10 | 18 | 20 | 26 | 12 | 8 | 6 | | | | 100 |
| 69 | 10 | 15 | 18 | 25 | 14 | 10 | 8 | | | | 100 |
| 70 | 12 | 14 | 16 | 18 | 16 | 12 | 8 | 4 | | | 100 |
| 71 | 10 | 12 | 14 | 16 | 24 | 12 | 8 | 4 | | | 100 |
| 72 | 8 | 12 | 14 | 16 | 18 | 16 | 12 | 4 | | | 100 |
| 73 | 8 | 10 | 12 | 16 | 18 | 16 | 14 | 6 | | | 100 |
| 74 | 8 | 10 | 11 | 12 | 13 | 22 | 18 | 6 | | | 100 |
| 75 | 8 | 7 | 12 | 11 | 13 | 22 | 18 | 6 | 3 | | 100 |
| 76 | 6 | 7 | 11 | 12 | 13 | 21 | 19 | 6 | 3 | 2 | 100 |
| 77 | 6 | 6 | 9 | 12 | 13 | 20 | 20 | 7 | 4 | 3 | 100 |
| 78 | 6 | 6 | 9 | 9 | 12 | 19 | 21 | 8 | 6 | 4 | 100 |
| 79 | 6 | 6 | 9 | 9 | 9 | 15 | 22 | 11 | 8 | 5 | 100 |
| 80 | 6 | 6 | 9 | 9 | 9 | 12 | 15 | 17 | 9 | 8 | 100 |

Filas que no suman 100: **60** (la misma errata está en el libro), **65** (el libro dice 20 en la zona de 70 %) y **66** (el libro dice 12 en la zona de 75 %). **[decisión de diseño]** Se cargan las filas 65 y 66 corregidas según el libro, y todas las filas se normalizan al calcular (6.2).

### 11.4 Volúmenes de pliometría (contactos por día)

| Nivel | Semana | D1 | D2 | D3 |
|---|---|---|---|---|
| 1 | 1 | 250 | 300 | 250 |
| 1 | 2 | 300 | 250 | 300 |
| 1 | 3 | 300 | 300 | 300 |
| 1 | 4 | 350 | 300 | 350 |
| 2 | 1 | 250 | 250 | 200 |
| 2 | 2 | 300 | 250 | 250 |
| 2 | 3 | 300 | 300 | 250 |
| 2 | 4 | 300 | 300 | 300 |
| 3 | 1 | 250 | 300 | — |
| 3 | 2 | 300 | 300 | — |
| 3 | 3 | 350 | 300 | — |
| 3 | 4 | 350 | 350 | — |

Listas de ejercicios: hoja **Pliometria** del Excel (Nivel 1 A, B y C con 6 ejercicios cada una; los niveles 2 y 3 están vacíos y los completa el usuario).

### 11.5 Plantillas de hipertrofia y fuerza básica

Celdas en orden semana 1 día 1, semana 1 día 2, semana 2 día 1, …

**2 días por semana**

| Sem. | Hipertrofia | Fuerza básica |
|---|---|---|
| 2 | 4x12, 4x10 · 4x10, 3x8 | 3x5-75/80%, 3x3-90% · 3x4-80/85%, 3x2-95% |
| 3 | 4x12, 4x10 · 4x10, 3x12 · 3x12, 3x8 | 3x5-75/80%, 3x3-90% · 3x6-70/75%, 3x4-80/85% · 3x4-80/85%, 3x2-95% |
| 4 | 4x12, 4x10 · 4x10, 3x12 · 3x12, 3x10 · 4x10, 3x8 | 3x6-70/75%, 3x4-80/85% · 3x5-75/80%, 3x3-90% · 3x6-70/75%, 3x4-80/85% · 3x4-80/85%, 3x2-95% |
| 5 | 4x12, 4x10 · 4x10, 3x12 · 3x12, 3x10 · 4x10, 3x12 · 4x10, 3x8 | 3x6-70/75%, 3x4-80/85% · 3x5-75/80%, 3x3-90% · 3x6-70/75%, 3x4-80/85% · 3x4-80/85%, 3x2-95% · 3x6-70/75%, 3x4-80/85% |

**1 día por semana**

| Sem. | Hipertrofia | Fuerza básica |
|---|---|---|
| 2 | 4x12 · 4x10 | 4x5-85% · 4x3-90% |
| 3 | 4x12 · 4x10 · 4x8 | 4x6-70% · 4x5-80% · 4x2-95% |
| 4 | 4x12 · 4x10 · 4x8 · 4x10 | 4x6-70% · 4x4-85% · 4x5-80% · 4x2-95% |
| 5 | 4x12 · 4x10 · 4x8 · 4x10 · 4x8 | 4x6-70% · 4x4-85% · 4x5-80% · 4x3-90% · 4x2-95% |

### 11.6 Ejercicios y 1RM iniciales (hoja Ejercicios)

Se importan los 32 ejercicios de la hoja con su 1RM y objetivo. **[decisión de diseño]** El tipo de cada uno lo asigna el constructor por nombre (arranques, cargadas y empujes → potencia; sentadillas, press, tirones, dominadas, fondos, remos y búlgaras → fuerza; bíceps y tríceps → hipertrofia), y el usuario lo corrige en la ficha.
