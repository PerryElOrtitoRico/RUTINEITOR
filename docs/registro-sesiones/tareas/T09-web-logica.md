# T09 — App del celular: lógica (`logica.js`)

## Reglas para las tareas de la app web
- JavaScript moderno (ES2020) **sin librerías, sin npm, sin herramientas de compilación**. Tiene que andar con solo abrir el archivo en Chrome.
- Archivo **clásico** (no módulo, sin `import`/`export`): define **un único** objeto global `const Logica = { ... };` con todas las funciones.
- Funciones puras: no tocan el DOM ni `localStorage`, y no modifican sus argumentos (devuelven objetos nuevos).
- Nombres en español, comentarios en español.

## Objetivo
Las funciones que usa la pantalla (T10) y que se prueban solas. Leé las **secciones 3, 5 y 6 de la especificación**.

## Archivos
- Crear `celular/logica.js` (carpeta `celular` en la **raíz** del repo, al lado de `pom.xml`).

## Qué escribir
```js
const Logica = {
  /** Misma gramática y reglas que la sección 3 (igual que NotacionRegistro de Java).
   *  planificadas: [{reps, kg}]. Devuelve [{reps, kg}]. Lanza Error con mensaje en español si es inválido. */
  parseNotacion(texto, planificadas) {},

  /** true/false, sin lanzar. */
  esNotacionValida(texto, planificadas) {},

  /** Valida un plan ya parseado (objeto): formato "rutineitor-plan", version 1, sesiones es un array.
   *  Devuelve el mismo objeto o lanza Error. */
  validarPlan(plan) {},

  /** La sesión del plan con ese microciclo y día, o null. */
  sesionDelPlan(plan, microciclo, dia) {},

  /** Registro vacío para ese plan: { formato: "rutineitor-registro", version: 1, perfil, mesocicloId, sesiones: [] } */
  registroVacio(plan) {},

  /** La sesión del registro con ese microciclo y día, o null. */
  sesionDelRegistro(registro, microciclo, dia) {},

  /** Devuelve un registro NUEVO con la sesión agregada o reemplazada (misma clave microciclo+día),
   *  las sesiones ordenadas por (microciclo, dia) y la sesión pasada por limpiarSesion. */
  guardarSesion(registro, sesion) {},

  /** Copia de la sesión sin ruido: saca de cada ejercicio las filas con texto vacío o solo espacios (y recorta
   *  los espacios del resto), y saca los ejercicios que quedan sin filas y con esfuerzo null. */
  limpiarSesion(sesion) {},

  /** Pista gris para la casilla: notación de las reps y los kg distintos en orden, con coma decimal.
   *  [{3,120},{3,120},{3,127.5}] → "3.3.3 · 120/127,5"; [{12,0},{12,0}] → "12.12"; [] → "". */
  pista(series) {},

  /** "registro-<perfil>-<inicio>.json", con los espacios del perfil cambiados por "_". */
  nombreArchivoRegistro(plan) {},
};
```

## Reglas
- `parseNotacion`: exactamente las de la sección 3 de la especificación: espacios ignorados, `0` → `[]`, reps 1..99, kg > 0 con coma decimal, peso de la serie en la misma posición o de la última, error "indicá el peso" si no hay planificadas. `"3-80.5"` → `[{reps:3,kg:80},{reps:5,kg:<peso de la 2.ª>}]`.
- Una sesión del registro tiene la forma de la sección 6: `{ microciclo, dia, fecha, descanso, final, ejercicios: [{ bloque, esfuerzo, filas: { etiqueta: texto } }] }`.

## Cómo verificar
Abrir en Chrome `docs/registro-sesiones/evaluacion/web/pruebas.html` (doble click o `file://…`): tiene que decir **"Todas las pruebas pasaron"**. Esa página la escribió otra persona, y no la tenés que modificar para que pase.

## No hagas
- No uses `export` ni `module`: el archivo se carga con `<script src="logica.js">`.
- No uses `parseFloat` sobre el kg sin cambiar la coma por punto, ni aceptes `"80abc"` como número.
