package com.rutina.service.datos;

import com.rutina.model.Ejercicio;
import com.rutina.model.Perfil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class PerfilRepo {

    private final Database db;

    public PerfilRepo(Database db) {
        this.db = db;
    }

    public List<Perfil> listar() throws SQLException {
        List<Perfil> res = new ArrayList<>();
        try (Statement st = db.conexion().createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nombre FROM perfil ORDER BY id")) {
            while (rs.next()) res.add(new Perfil(rs.getLong(1), rs.getString(2)));
        }
        return res;
    }

    /**
     * Crea el perfil con los ejercicios iniciales. El primer perfil (el usuario) recibe los 1RM de su
     * planilla; los siguientes arrancan sin 1RM.
     */
    public Perfil crear(String nombre) throws SQLException {
        boolean primero = listar().isEmpty();
        long id;
        try (PreparedStatement ps = db.conexion().prepareStatement("INSERT INTO perfil (nombre) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        EjercicioRepo ejercicios = new EjercicioRepo(db);
        for (Ejercicio e : DatosIniciales.ejercicios()) {
            ejercicios.guardar(new Ejercicio(0, id, e.nombre(), e.tipo(), primero ? e.rm() : null,
                    primero ? e.rmObjetivo() : null, e.techoPct(), e.repsPorSerie(), e.grupoPlio(), e.orden(), e.activo()));
        }
        return new Perfil(id, nombre);
    }
}
