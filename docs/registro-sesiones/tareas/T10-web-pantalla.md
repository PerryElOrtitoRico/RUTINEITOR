# T10 — App del celular: pantalla (`index.html`)

## Reglas para las tareas de la app web
Las mismas de T09: sin librerías ni compilación; nombres y comentarios en español. Todo el cálculo pasa por el objeto `Logica` de `logica.js` (no repitas esa lógica acá).

## Objetivo
La pantalla que el usuario usa en el gimnasio: cargar el plan, elegir microciclo y día, y completar la sesión. Se basa en la planilla del usuario: una columna con qué se evalúa y otra con la casilla o el desplegable.

## Archivos
- Crear `celular/index.html` (HTML, `<style>` y `<script>` adentro), que carga `<script src="logica.js"></script>` antes de su propio script.

## Datos guardados (`localStorage`)
| clave | valor |
|---|---|
| `rutineitor.planes` | objeto JSON `{ "<mesocicloId>": plan, … }` |
| `rutineitor.registros` | objeto JSON `{ "<mesocicloId>": registro, … }` |
| `rutineitor.activo` | el mesocicloId elegido (texto) |

Todo acceso a `localStorage` va dentro de `try/catch`: si falla, se muestra un aviso arriba ("No se pueden guardar datos en este navegador").

## Pantalla de inicio
- Título "RUTINEITOR · Registro".
- Botón **"Cargar plan"** (`<input type="file" accept=".json,application/json">`):
  - lee el archivo con `FileReader`, `JSON.parse` y `Logica.validarPlan`;
  - si es válido, lo guarda en `rutineitor.planes` (reemplaza si ya existía ese mesocicloId), crea un registro vacío si no había y lo deja activo;
  - si no es válido, muestra el mensaje de error.
- Selector del **plan activo**, si hay más de uno: texto "Franco · desde 26/10/2026".
- Selectores **Microciclo** (1..microciclos) y **Día** (1..dias), y botón **"Abrir sesión"**.
- Resumen: "Sesiones anotadas: N de M".
- Botón **"Exportar registro"** (T11) y botón **"Borrar este plan"** (pide confirmación).

## Pantalla de sesión
De arriba hacia abajo, una sola columna, cómoda en un celular de 360 px de ancho y con letra de 16 px o más:
1. Encabezado: "Microciclo X · Día Y" y botón "← Volver".
2. **Descanso previo**: `<select>` con una opción vacía ("—") y las 6 opciones de la sección 2 de la especificación, de 6 a 1, con el texto `"6 · Bien descansado y con ganas de entrenar"`.
3. Por cada ejercicio de `Logica.sesionDelPlan(...)`:
   - el nombre en negrita;
   - **Sensación del ejercicio**: `<select>` con "—" y las 5 opciones;
   - una fila por cada `fila` del plan: a la izquierda la etiqueta (`80-85`); a la derecha un `<input type="text" inputmode="decimal">` con `placeholder = Logica.pista(fila.series)` y el valor guardado (si lo hay).
4. **Sensación al terminar**: `<select>` con "—" y las 5 opciones.
5. Si la sesión del plan no tiene ejercicios, el texto "Este día no tiene ejercicios".

## Comportamiento
- **Guardado automático** en cada cambio (`change` en los select, `input` en los textos):
  - se arma la sesión del registro con lo que hay en pantalla;
  - si no tenía `fecha`, se le pone la de hoy (`AAAA-MM-DD`, hora local);
  - se guarda con `Logica.guardarSesion` en `rutineitor.registros`.
  - Una sesión en la que no se tocó nada **no** se guarda.
- **Validación en vivo**: si `!Logica.esNotacionValida(texto, fila.series)` y el texto no está vacío, el input se pinta con borde rojo y debajo aparece el mensaje de error de `parseNotacion`. Se guarda igual (para no perder lo escrito); el error lo detecta la PC al importar.
- Si el texto es válido pero tiene **más series que las planificadas**, debajo aparece en gris: "Ojo: N series (planificadas: M). Los decimales del peso van con coma."
- Botón **"Borrar lo anotado en esta sesión"**, con confirmación: la saca del registro.

## Cómo verificar
- Abrir `celular/index.html` en Chrome de la PC (doble click) y cargar `docs/registro-sesiones/ejemplos/plan-ejemplo.json`.
- Abrir microciclo 1, día 1: aparecen "Sentadilla" con las filas `70` y `80-85` (pista `3.3.3 · 120/127,5`) y "Biceps con mancuerna" con `4x12`.
- Elegir el descanso, escribir `3.3-115` en `80-85`, volver y recargar la página: los datos siguen ahí.
- Escribir `3..3`: borde rojo y mensaje.
- En las herramientas de desarrollo (F12 → modo dispositivo, 360 × 740), todo se lee sin scroll horizontal.
