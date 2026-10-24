# T02 — Leer la notación de lo realizado

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, `mvn -q test` en verde al terminar.

## Objetivo
Convertir el texto que el usuario escribe en una casilla (por ejemplo `3.3-85.3-80`) en la lista de series realizadas. La gramática y los ejemplos están en la **sección 3 de la especificación**: leela completa antes de empezar.

## Archivos
- Crear `src/main/java/com/rutina/registro/SerieReal.java`
- Crear `src/main/java/com/rutina/registro/NotacionRegistro.java`
- Crear `src/test/java/com/rutina/registro/NotacionRegistroTest.java`

## Qué escribir
```java
package com.rutina.registro;

/** Una serie: repeticiones y kg (0 = peso desconocido). Se usa para lo planificado y para lo realizado. */
public record SerieReal(int reps, double kg) { }
```

```java
package com.rutina.registro;

public final class NotacionRegistro {
    private NotacionRegistro() { }

    /**
     * @param texto        lo que escribió el usuario; no puede ser vacío (vacío = "como lo planificado", lo resuelve quien llama)
     * @param planificadas series planificadas de esa fila, en orden (puede ser una lista vacía)
     * @return las series realizadas; lista vacía si el texto es "0"
     * @throws IllegalArgumentException si el texto no cumple la gramática; el mensaje dice qué está mal
     */
    public static List<SerieReal> parse(String texto, List<SerieReal> planificadas) { ... }

    /** true si parse no lanzaría excepción. */
    public static boolean esValida(String texto, List<SerieReal> planificadas) { ... }
}
```

## Reglas
- Antes de analizar, quitá todos los espacios.
- `texto` null o vacío → `IllegalArgumentException`.
- `"0"` → `List.of()`.
- Se separa por `.`. Cada parte es `reps` o `reps-kg`.
  - reps: solo dígitos, valor de 1 a 99 (sin signo; `00` y `0` dentro de una lista son inválidos).
  - kg: número mayor que 0, con decimales opcionales **con coma** (`82,5`). Como el punto ya separa series, `3-80.5` se lee como dos series: `(3, 80)` y `(5, peso planificado de la 2.ª)`. Eso es correcto, no un error.
- Peso de una serie sin `-kg`: el de `planificadas.get(i)` (i = posición de la serie, desde 0). Si `i >= planificadas.size()`, el de la última planificada. Si `planificadas` está vacía → excepción con el texto "indicá el peso".
- Una parte vacía (`3..3`, `.3`, `3.`) es inválida.

## Tests a escribir (como mínimo)
Con `var plan = List.of(new SerieReal(3, 120), new SerieReal(3, 120), new SerieReal(3, 120));`

| texto | resultado |
|---|---|
| `3.3-85.3-80` | `[(3,120), (3,85), (3,80)]` |
| `3.3` | `[(3,120), (3,120)]` |
| `2.3` | `[(2,120), (3,120)]` |
| `3.3.3.3` | cuatro series `(3,120)` |
| `3-82,5` | `[(3,82.5)]` |
| ` 3 . 3 - 85 ` | `[(3,120), (3,85)]` |
| `0` | `[]` |
| `3`, con `planificadas = List.of()` | excepción que contiene "indicá el peso" |
| `3-90`, con `planificadas = List.of()` | `[(3,90)]` |
| `3-80.5` | `[(3,80), (5,120)]` |

Inválidos (excepción): `""`, `3..3`, `3-`, `-80`, `0.3`, `100`, `3-0`, `a`, `3.`.

Además, `esValida("3.3", plan)` es `true` y `esValida("3..3", plan)` es `false`.

## Cómo verificar
`mvn -q test` → sin errores.

## No hagas
- No uses `Double.parseDouble` directamente sobre el kg sin cambiar la coma por punto.
- No aceptes `0` mezclado con otras series (`0.3` es inválido).
