# T08 — Botones y pestaña "Registro" en la PC

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, nombres y comentarios en español, sin dependencias nuevas, `mvn -q test` en verde al terminar. Esta tarea es de interfaz (Swing): no lleva tests automáticos, sino la prueba manual del final.

## Objetivo
En la pestaña **Mesociclo**:
1. un botón **"Plan al celular"** que guarda el JSON del plan;
2. un botón **"Importar registro"** que lee el JSON del celular y lo guarda;
3. una pestaña nueva, **"Registro"**, en el panel de la derecha, con la tabla de planilla vs entrenado.

## Código que ya existe (lo vas a modificar)
`src/main/java/com/rutina/ui/Contexto.java`: guarda lo que comparten las pestañas.
```java
public final class Contexto {
    final Database db; final Perfil perfil; final EjercicioRepo ejercicios; final MesocicloRepo mesociclos;
    final PlanPlioRepo planes; final MetodologiaRepo metodologiaRepo;
    Contexto(Database db, Perfil perfil) throws SQLException { /* crea cada repo con new XRepo(db) */ }
    Metodologia metodologia();
    static void error(Component padre, Exception e);   // muestra el mensaje en un diálogo
}
```
`src/main/java/com/rutina/ui/PanelMesociclo.java` (fragmentos):
```java
private Mesociclo actual;                 // mesociclo elegido; puede ser null
private ResultadoMesociclo resultado;     // null si todavía no se puede calcular
// en el constructor:
JButton exportar = new JButton("Exportar PDF");
exportar.addActionListener(e -> exportar());
arriba.add(exportar);
JTabbedPane derecha = new JTabbedPane();
derecha.addTab("Contadores", new JScrollPane(new JTable(contadores)));
derecha.addTab("Gráfico", grafico);
// recalcular() se llama cada vez que cambia el mesociclo; al final llama a dibujarGrilla().
// exportar() usa un JFileChooser que arranca en ~/Documentos; copiá ese estilo.
```
De tareas anteriores: `ArmadorPlan.armar(perfil, mes, res, m)`, `JsonRegistro.escribirPlan(path, plan)`, `JsonRegistro.leerRegistro(path)`, `RegistroRepo(db)` con `guardar`/`cargar`, `AnalisisRegistro.analizar(plan, registro)`, `ResumenSesion` y `ComparacionTipo`.

## Archivos
- Modificar `Contexto.java`: agregar `final RegistroRepo registros;` y crearlo en el constructor.
- Modificar `PanelMesociclo.java`.

## Qué hacer
1. **"Plan al celular"** (al lado de "Exportar PDF"):
   - si `resultado == null`, avisá "El mesociclo todavía no se puede calcular" y no hagas nada más;
   - si no, `JFileChooser` con nombre sugerido `plan-<perfil>-<inicio>.json`, después `JsonRegistro.escribirPlan(...)` con `ArmadorPlan.armar(ctx.perfil.nombre(), actual, resultado, ctx.metodologia())`;
   - errores con `Contexto.error`.
2. **"Importar registro"**:
   - `JFileChooser` → `JsonRegistro.leerRegistro`;
   - si `registro.mesocicloId() != actual.id()`, mostrá un error que diga que el registro es de otro mesociclo y no guardes;
   - si es del mismo, `ctx.registros.guardar(registro)` y `recalcular()`.
3. **Pestaña "Registro"** (después de "Gráfico"):
   - un `JTable` con las columnas `Micro, Día, Fecha, Descanso, Final, Esfuerzo, Tipo, Vol planilla, Vol entrenado, IMR planilla, IMR entrenado`;
   - una fila por cada `ComparacionTipo` de cada `ResumenSesion`;
   - descanso y final se muestran como texto de la escala (por ejemplo `5 · Bien descansado`), con `EscalaDescanso.deValor(...)` y `EscalaFinal.deValor(...)`;
   - los IMR con 1 decimal; los `null`, como celda vacía.
4. La tabla se vuelve a llenar al final de `recalcular()`:
   - si hay `resultado` y `ctx.registros.cargar(actual.id())` tiene algo, con `AnalisisRegistro.analizar(ArmadorPlan.armar(...), registro)`;
   - si no, vacía;
   - si `analizar` lanza una excepción, mostrá su mensaje en el label de avisos que ya existe (`avisos.setText(...)`) en vez de la tabla.

## Prueba manual (anotá el resultado)
1. `mvn -q package` y abrir `java -jar target/rutina-1.0.0-jar-with-dependencies.jar`.
2. Elegir un mesociclo armado y tocar "Plan al celular": se genera el `.json` y, abierto con un editor, tiene la forma de la sección 5.
3. Editar a mano una copia de `docs/registro-sesiones/ejemplos/registro-ejemplo.json` para que su `mesocicloId` sea el del mesociclo elegido y las etiquetas coincidan con su plan, e importarla: la pestaña "Registro" muestra las filas.
4. Importar un registro con otro `mesocicloId`: aparece el error y la tabla no cambia.
5. Cerrar y volver a abrir el programa: la pestaña "Registro" sigue mostrando lo importado.
