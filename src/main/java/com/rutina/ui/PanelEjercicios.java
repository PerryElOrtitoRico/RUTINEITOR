package com.rutina.ui;

import com.rutina.model.Ejercicio;
import com.rutina.model.Tipo;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pestaña Ejercicios: Potencia, Fuerza, Hipertrofia (con ficha) y Pliometría (listas por nivel y día). */
final class PanelEjercicios extends JPanel {

    private final Contexto ctx;
    private final List<SeccionTabla> secciones = new ArrayList<>();
    private final SeccionPlio plio;

    PanelEjercicios(Contexto ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        JTabbedPane tabs = new JTabbedPane();
        for (Tipo t : List.of(Tipo.POTENCIA, Tipo.FUERZA, Tipo.HIPERTROFIA)) {
            SeccionTabla s = new SeccionTabla(t);
            secciones.add(s);
            tabs.addTab(titulo(t), s);
        }
        plio = new SeccionPlio();
        tabs.addTab("Pliometría", plio);
        add(tabs, BorderLayout.CENTER);
        ctx.alCambiar(this::refrescar);
        refrescar();
    }

    private void refrescar() {
        try {
            List<Ejercicio> todos = ctx.ejercicios.listar(ctx.perfil.id());
            secciones.forEach(s -> s.cargar(todos));
            plio.cargar(todos);
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    static String titulo(Tipo t) {
        return switch (t) {
            case POTENCIA -> "Potencia";
            case FUERZA -> "Fuerza";
            case HIPERTROFIA -> "Hipertrofia";
            case PLIOMETRIA -> "Pliometría";
        };
    }

    private static String kg(Double v) {
        return v == null ? "" : (v % 1 == 0 ? String.valueOf(v.intValue()) : String.valueOf(v));
    }

    /** Lista de un tipo; doble click o "Editar" abre la ficha. */
    private final class SeccionTabla extends JPanel {
        private final Tipo tipo;
        private final DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"Ejercicio", "1RM (kg)", "Objetivo (kg)", "Techo", "Activo"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        private final JTable tabla = new JTable(modelo);
        private List<Ejercicio> filas = List.of();

        SeccionTabla(Tipo tipo) {
            super(new BorderLayout());
            this.tipo = tipo;
            tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tabla.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) editar();
                }
            });
            add(new JScrollPane(tabla), BorderLayout.CENTER);
            JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton agregar = new JButton("Agregar");
            agregar.addActionListener(e -> agregar());
            JButton editar = new JButton("Editar ficha");
            editar.addActionListener(e -> editar());
            JButton activar = new JButton("Activar / desactivar");
            activar.addActionListener(e -> activar());
            botones.add(agregar);
            botones.add(editar);
            botones.add(activar);
            botones.add(new JLabel("  Doble click para abrir la ficha (1RM, techo, repeticiones por zona, gráfico)."));
            add(botones, BorderLayout.SOUTH);
        }

        void cargar(List<Ejercicio> todos) {
            filas = todos.stream().filter(e -> e.tipo() == tipo).toList();
            modelo.setRowCount(0);
            for (Ejercicio e : filas) {
                modelo.addRow(new Object[]{e.nombre(), kg(e.rm()), kg(e.rmObjetivo()),
                        tipo == Tipo.HIPERTROFIA ? "" : e.techoPct() + " %", e.activo() ? "Sí" : "No"});
            }
        }

        private Ejercicio seleccionado() {
            int r = tabla.getSelectedRow();
            return r < 0 ? null : filas.get(tabla.convertRowIndexToModel(r));
        }

        private void agregar() {
            String nombre = JOptionPane.showInputDialog(this, "Nombre del ejercicio:");
            if (nombre == null || nombre.isBlank()) return;
            try {
                ctx.ejercicios.guardar(new Ejercicio(0, ctx.perfil.id(), nombre.trim(), tipo, null, null, 100,
                        Map.of(), null, filas.size(), true));
                ctx.cambio();
            } catch (Exception e) {
                Contexto.error(this, e);
            }
        }

        private void editar() {
            Ejercicio e = seleccionado();
            if (e == null) return;
            if (new FichaEjercicio(PanelEjercicios.this, ctx, e).mostrar()) ctx.cambio();
        }

        private void activar() {
            Ejercicio e = seleccionado();
            if (e == null) return;
            try {
                ctx.ejercicios.guardar(new Ejercicio(e.id(), e.perfilId(), e.nombre(), e.tipo(), e.rm(), e.rmObjetivo(),
                        e.techoPct(), e.repsPorSerie(), e.grupoPlio(), e.orden(), !e.activo()));
                ctx.cambio();
            } catch (Exception ex) {
                Contexto.error(this, ex);
            }
        }
    }

    /** Listas de pliometría por grupo (1A … 3B), reordenables. */
    private final class SeccionPlio extends JPanel {
        private static final String[] GRUPOS = {"1A", "1B", "1C", "2A", "2B", "2C", "3A", "3B"};
        private final JComboBox<String> grupo = new JComboBox<>(GRUPOS);
        private final DefaultListModel<String> modelo = new DefaultListModel<>();
        private final JList<String> lista = new JList<>(modelo);
        private List<Ejercicio> todos = List.of();
        private List<Ejercicio> filas = List.of();

        SeccionPlio() {
            super(new BorderLayout());
            JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
            arriba.add(new JLabel("Grupo (nivel + día):"));
            arriba.add(grupo);
            arriba.add(new JLabel("  Cada día usa 6 ejercicios: si hay menos se repite la lista, si hay más se toman los 6 primeros."));
            grupo.addActionListener(e -> mostrar());
            add(arriba, BorderLayout.NORTH);
            lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane sp = new JScrollPane(lista);
            sp.setBorder(BorderFactory.createTitledBorder("Ejercicios activos del grupo, en orden"));
            add(sp, BorderLayout.CENTER);
            JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
            for (String[] b : new String[][]{{"Agregar", "a"}, {"Renombrar", "r"}, {"Subir", "s"}, {"Bajar", "b"}, {"Desactivar", "d"}}) {
                JButton boton = new JButton(b[0]);
                boton.addActionListener(e -> accion(b[1]));
                botones.add(boton);
            }
            add(botones, BorderLayout.SOUTH);
        }

        void cargar(List<Ejercicio> todos) {
            this.todos = todos;
            mostrar();
        }

        private void mostrar() {
            String g = (String) grupo.getSelectedItem();
            filas = todos.stream().filter(e -> e.tipo() == Tipo.PLIOMETRIA && e.activo() && g.equals(e.grupoPlio()))
                    .sorted((a, b) -> Integer.compare(a.orden(), b.orden())).toList();
            modelo.clear();
            filas.forEach(e -> modelo.addElement(e.nombre()));
        }

        private void accion(String a) {
            int i = lista.getSelectedIndex();
            try {
                switch (a) {
                    case "a" -> {
                        String n = JOptionPane.showInputDialog(this, "Ejercicio:");
                        if (n == null || n.isBlank()) return;
                        int orden = filas.stream().mapToInt(Ejercicio::orden).max().orElse(-1) + 1;
                        ctx.ejercicios.guardar(new Ejercicio(0, ctx.perfil.id(), n.trim(), Tipo.PLIOMETRIA, null, null,
                                100, Map.of(), (String) grupo.getSelectedItem(), orden, true));
                    }
                    case "r" -> {
                        if (i < 0) return;
                        Ejercicio e = filas.get(i);
                        String n = (String) JOptionPane.showInputDialog(this, "Nuevo nombre:", "Renombrar",
                                JOptionPane.PLAIN_MESSAGE, null, null, e.nombre());
                        if (n == null || n.isBlank()) return;
                        ctx.ejercicios.guardar(con(e, n.trim(), e.orden(), true));
                    }
                    case "s", "b" -> {
                        int j = a.equals("s") ? i - 1 : i + 1;
                        if (i < 0 || j < 0 || j >= filas.size()) return;
                        Ejercicio x = filas.get(i);
                        Ejercicio y = filas.get(j);
                        // se renumera todo para que el intercambio funcione aunque haya órdenes repetidos
                        List<Ejercicio> orden = new ArrayList<>(filas);
                        orden.set(i, y);
                        orden.set(j, x);
                        for (int k = 0; k < orden.size(); k++) {
                            Ejercicio e = orden.get(k);
                            ctx.ejercicios.guardar(con(e, e.nombre(), k, true));
                        }
                        ctx.cambio();
                        lista.setSelectedIndex(j);
                        return;
                    }
                    case "d" -> {
                        if (i < 0) return;
                        Ejercicio e = filas.get(i);
                        ctx.ejercicios.guardar(con(e, e.nombre(), e.orden(), false));
                    }
                    default -> {
                        return;
                    }
                }
                ctx.cambio();
            } catch (Exception ex) {
                Contexto.error(this, ex);
            }
        }

        private Ejercicio con(Ejercicio e, String nombre, int orden, boolean activo) {
            return new Ejercicio(e.id(), e.perfilId(), nombre, e.tipo(), e.rm(), e.rmObjetivo(), e.techoPct(),
                    e.repsPorSerie(), e.grupoPlio(), orden, activo);
        }
    }
}
