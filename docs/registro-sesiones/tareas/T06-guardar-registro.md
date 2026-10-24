# T06 — Guardar el registro en la base de datos

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, `mvn -q test` en verde al terminar.

## Objetivo
Guardar en SQLite el último registro importado de cada mesociclo. Si se vuelve a importar, **reemplaza** al anterior; no se duplica.

## Código que ya existe
`src/main/java/com/rutina/service/datos/Database.java` (extracto):
```java
public final class Database implements AutoCloseable {
    private static final String[] ESQUEMA = {
            "CREATE TABLE IF NOT EXISTS perfil (...)",
            // … otras tablas …
            "CREATE TABLE IF NOT EXISTS plan_pliometria (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " perfil_id INTEGER NOT NULL, nivel INTEGER NOT NULL, inicio TEXT NOT NULL)",
    };
    public static Database abrir(java.nio.file.Path archivo) throws IOException, SQLException { ... }
    public java.sql.Connection conexion() { ... }
}
```
Las demás clases de `service.datos` siguen este patrón (ejemplo real, `PlanPlioRepo`):
```java
public final class PlanPlioRepo {
    private final Database db;
    public PlanPlioRepo(Database db) { this.db = db; }
    public void borrar(long id) throws SQLException {
        try (PreparedStatement ps = db.conexion().prepareStatement("DELETE FROM plan_pliometria WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
```
De T05: `RegistroCelular`, `JsonRegistro.registroAJson(...)` y `JsonRegistro.registroDeJson(...)`.

## Archivos
- Modificar `Database.java`: **solo** agregar al final de `ESQUEMA` la línea
  `"CREATE TABLE IF NOT EXISTS registro (mesociclo_id INTEGER PRIMARY KEY, json TEXT NOT NULL, importado TEXT NOT NULL)"`
- Crear `src/main/java/com/rutina/service/datos/RegistroRepo.java`
- Crear `src/test/java/com/rutina/service/datos/RegistroRepoTest.java`

## Qué escribir
```java
package com.rutina.service.datos;

public final class RegistroRepo {
    public RegistroRepo(Database db) { ... }
    /** Inserta o reemplaza el registro de r.mesocicloId(); importado = fecha y hora actuales (ISO). */
    public void guardar(RegistroCelular r) throws SQLException { ... }
    public Optional<RegistroCelular> cargar(long mesocicloId) throws SQLException { ... }
    public void borrar(long mesocicloId) throws SQLException { ... }
}
```
Usá `INSERT OR REPLACE INTO registro (mesociclo_id, json, importado) VALUES (?, ?, ?)`.

## Tests a escribir (como mínimo)
Usá `@TempDir Path d` y `Database.abrir(d.resolve("r.db"))`.
- `cargar` de un mesociclo sin registro devuelve `Optional.empty()`.
- Guardar y cargar devuelve un registro igual (`assertEquals`).
- Guardar dos veces el mismo `mesocicloId` con distinto contenido: `cargar` devuelve el segundo, y la tabla tiene una sola fila (`SELECT COUNT(*) FROM registro`).
- Después de `borrar`, `cargar` devuelve `Optional.empty()`.
- Reabrir la base (otro `Database.abrir` sobre el mismo archivo) conserva el registro.

## Cómo verificar
`mvn -q test` → sin errores. Los tests que ya existían de `PersistenciaTest` también tienen que pasar.
