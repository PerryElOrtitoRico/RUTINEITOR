# T01 — Escalas del registro

## Reglas del proyecto (valen para todas las tareas)
- Java 21, Maven. Código en `src/main/java`, tests en `src/test/java` (JUnit 5: `org.junit.jupiter.api.Test`, `Assertions.*`).
- Nombres de clases, métodos y variables en español, igual que el resto del proyecto. Comentarios en español.
- No agregues dependencias al `pom.xml`. Ya están: Gson 2.10.1, sqlite-jdbc, OpenPDF, JFreeChart, FlatLaf.
- No modifiques archivos que la tarea no nombre.
- Al terminar, `mvn -q test` tiene que pasar entero (incluidos los tests que ya existían).

## Objetivo
Crear las tres escalas de la sección 2 de la especificación como `enum` de Java.

## Archivos
- Crear `src/main/java/com/rutina/registro/EscalaDescanso.java`
- Crear `src/main/java/com/rutina/registro/EscalaEsfuerzo.java`
- Crear `src/main/java/com/rutina/registro/EscalaFinal.java`
- Crear `src/test/java/com/rutina/registro/EscalasTest.java`

## Qué escribir
Paquete `com.rutina.registro`. Las tres enums tienen la misma forma:

```java
public enum EscalaDescanso {
    DESCANSADO_CON_GANAS(6, "Bien descansado y con ganas de entrenar"),
    DESCANSADO(5, "Bien descansado"),
    DESCANSADO_DISTRAIDO(4, "Bien descansado pero distraído"),
    MAL_DESCANSADO_CON_GANAS(3, "Mal descansado pero con ganas de entrenar"),
    MAL_DESCANSADO(2, "Mal descansado"),
    MAL_DESCANSADO_SIN_GANAS(1, "Mal descansado y sin ganas de entrenar");

    public int valor() { ... }
    public String texto() { ... }
    /** Lanza IllegalArgumentException si no existe ese valor. */
    public static EscalaDescanso deValor(int valor) { ... }
}
```

- `EscalaEsfuerzo`: `MUY_LIVIANO(1, "Muy liviano")`, `LIVIANO(2, "Liviano")`, `JUSTO(3, "Justo")`, `PESADO(4, "Pesado")`, `AL_LIMITE(5, "Al límite")`.
- `EscalaFinal`: `DESTRUIDO(1, "Destruido")`, `CANSADO(2, "Cansado")`, `NORMAL(3, "Normal")`, `BIEN(4, "Bien")`, `CON_ENERGIA(5, "Con energía")`.
- El mensaje de la excepción de `deValor` tiene que incluir el valor recibido.

## Tests a escribir (como mínimo)
```java
@Test void descansoDeValor() {
    assertEquals(EscalaDescanso.DESCANSADO_DISTRAIDO, EscalaDescanso.deValor(4));
    assertEquals("Bien descansado pero distraído", EscalaDescanso.deValor(4).texto());
}
@Test void valorInexistente() {
    assertThrows(IllegalArgumentException.class, () -> EscalaEsfuerzo.deValor(6));
    assertThrows(IllegalArgumentException.class, () -> EscalaDescanso.deValor(0));
}
```

## Cómo verificar
`mvn -q test` → sin errores.

## No hagas
- No uses `ordinal()` como valor de la escala: el descanso va de 6 a 1.
