package com.rutina.service.datos;

import com.rutina.registro.EjercicioRegistro;
import com.rutina.registro.RegistroCelular;
import com.rutina.registro.SesionRegistro;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T06. */
class T06RegistroRepoOcultoTest {

    @TempDir
    Path d;

    private static RegistroCelular registro(long meso, String fila) {
        return new RegistroCelular("rutineitor-registro", 1, "Franco", meso, List.of(
                new SesionRegistro(1, 1, "2026-10-27", 5, 4, List.of(new EjercicioRegistro("1-1", 3, Map.of("80-85", fila))))));
    }

    private static int filas(Database db) throws Exception {
        try (var st = db.conexion().createStatement(); var rs = st.executeQuery("SELECT COUNT(*) FROM registro")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void cicloCompleto() throws Exception {
        var db = Database.abrir(d.resolve("r.db"));
        var repo = new RegistroRepo(db);
        assertEquals(Optional.empty(), repo.cargar(3));
        repo.guardar(registro(3, "3.3"));
        repo.guardar(registro(4, "0"));
        assertEquals(Optional.of(registro(3, "3.3")), repo.cargar(3));
        repo.guardar(registro(3, "3-100"));
        assertEquals(Optional.of(registro(3, "3-100")), repo.cargar(3));
        assertEquals(2, filas(db));
        repo.borrar(3);
        assertEquals(Optional.empty(), repo.cargar(3));
        assertEquals(Optional.of(registro(4, "0")), repo.cargar(4));
    }

    @Test
    void persisteAlReabrir() throws Exception {
        new RegistroRepo(Database.abrir(d.resolve("r.db"))).guardar(registro(5, "2.2"));
        assertEquals(Optional.of(registro(5, "2.2")), new RegistroRepo(Database.abrir(d.resolve("r.db"))).cargar(5));
    }

    @Test
    void columnaImportado() throws Exception {
        var db = Database.abrir(d.resolve("r.db"));
        new RegistroRepo(db).guardar(registro(6, "3"));
        try (var st = db.conexion().createStatement(); var rs = st.executeQuery("SELECT importado FROM registro WHERE mesociclo_id = 6")) {
            assertTrue(rs.next());
            assertTrue(rs.getString(1).startsWith("20"), rs.getString(1));
        }
    }

    @Test
    void lasDemasTablasSiguen() throws Exception {
        var db = Database.abrir(d.resolve("r.db"));
        assertEquals(10, new MetodologiaRepo(db).cargar().variantes().size());
        assertNotNull(new PerfilRepo(db).crear("X"));
    }
}
