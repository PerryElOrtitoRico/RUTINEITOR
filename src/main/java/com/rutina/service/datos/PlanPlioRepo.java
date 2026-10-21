package com.rutina.service.datos;

import com.rutina.model.PlanPliometria;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class PlanPlioRepo {

    private final Database db;

    public PlanPlioRepo(Database db) {
        this.db = db;
    }

    public List<PlanPliometria> listar(long perfilId) throws SQLException {
        List<PlanPliometria> res = new ArrayList<>();
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "SELECT id, perfil_id, nivel, inicio FROM plan_pliometria WHERE perfil_id = ? ORDER BY inicio, id")) {
            ps.setLong(1, perfilId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new PlanPliometria(rs.getLong(1), rs.getLong(2), rs.getInt(3), LocalDate.parse(rs.getString(4))));
                }
            }
        }
        return res;
    }

    public PlanPliometria guardar(PlanPliometria p) throws SQLException {
        if (p.id() == 0) {
            try (PreparedStatement ps = db.conexion().prepareStatement(
                    "INSERT INTO plan_pliometria (perfil_id, nivel, inicio) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, p.perfilId());
                ps.setInt(2, p.nivel());
                ps.setString(3, p.inicio().toString());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    return new PlanPliometria(rs.getLong(1), p.perfilId(), p.nivel(), p.inicio());
                }
            }
        }
        try (PreparedStatement ps = db.conexion().prepareStatement(
                "UPDATE plan_pliometria SET nivel = ?, inicio = ? WHERE id = ?")) {
            ps.setInt(1, p.nivel());
            ps.setString(2, p.inicio().toString());
            ps.setLong(3, p.id());
            ps.executeUpdate();
        }
        return p;
    }

    public void borrar(long id) throws SQLException {
        try (PreparedStatement ps = db.conexion().prepareStatement("DELETE FROM plan_pliometria WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
