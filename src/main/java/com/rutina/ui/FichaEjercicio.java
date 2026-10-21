package com.rutina.ui;

import com.rutina.model.Ejercicio;
import com.rutina.model.Tipo;
import com.rutina.model.Zona;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.Map;

/** Ficha de un ejercicio: 1RM, objetivo, techo, repeticiones por serie propias y evolución del 1RM. */
final class FichaEjercicio extends JDialog {

    private final Contexto ctx;
    private final Ejercicio original;
    private final JTextField nombre = new JTextField(24);
    private final JComboBox<Tipo> tipo = new JComboBox<>(new Tipo[]{Tipo.POTENCIA, Tipo.FUERZA, Tipo.HIPERTROFIA});
    private final JTextField rm = new JTextField(6);
    private final JTextField objetivo = new JTextField(6);
    private final JSpinner techo = new JSpinner(new SpinnerNumberModel(100, 55, 100, 5));
    private final Map<Zona, Integer> propias;
    private boolean guardado;

    FichaEjercicio(Component padre, Contexto ctx, Ejercicio e) {
        super(SwingUtilities.getWindowAncestor(padre), "Ficha: " + e.nombre(), ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        this.original = e;
        this.propias = new EnumMap<>(Zona.class);
        if (e.repsPorSerie() != null) propias.putAll(e.repsPorSerie());

        nombre.setText(e.nombre());
        tipo.setSelectedItem(e.tipo());
        rm.setText(e.rm() == null ? "" : String.valueOf(e.rm()));
        objetivo.setText(e.rmObjetivo() == null ? "" : String.valueOf(e.rmObjetivo()));
        techo.setValue(e.techoPct());

        JPanel datos = new JPanel(new GridBagLayout());
        datos.setBorder(BorderFactory.createTitledBorder("Datos"));
        fila(datos, 0, "Nombre", nombre);
        fila(datos, 1, "Tipo", tipo);
        fila(datos, 2, "1RM actual (kg)", rm);
        fila(datos, 3, "1RM objetivo (kg)", objetivo);
        fila(datos, 4, "Techo de intensidad (%)", techo);
        JLabel ayuda = new JLabel("<html>El techo es la zona más alta que recibe este ejercicio en la aproximación compleja;<br>"
                + "lo que está por encima pasa a los otros ejercicios del mismo tipo de ese día.</html>");
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        c.insets = new Insets(4, 4, 4, 4);
        datos.add(ayuda, c);

        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.add(datos, BorderLayout.NORTH);
        if (e.tipo() != Tipo.HIPERTROFIA) izquierda.add(tablaReps(), BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.add(izquierda, BorderLayout.WEST);
        try {
            centro.add(Graficos.evolucionRm(ctx.ejercicios.historial(e.id()), e.rmObjetivo()), BorderLayout.CENTER);
        } catch (Exception ex) {
            Contexto.error(padre, ex);
        }

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(ev -> dispose());
        JButton guardar = new JButton("Guardar");
        guardar.addActionListener(ev -> guardar());
        botones.add(cancelar);
        botones.add(guardar);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().setDefaultButton(guardar);
        pack();
        setLocationRelativeTo(padre);
    }

    /** true si se guardaron cambios. */
    boolean mostrar() {
        setVisible(true);
        return guardado;
    }

    private JComponent tablaReps() {
        AbstractTableModel modelo = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return Zona.values().length;
            }

            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public String getColumnName(int c) {
                return new String[]{"Zona", "Reps por serie", "Origen"}[c];
            }

            @Override
            public Object getValueAt(int r, int c) {
                Zona z = Zona.values()[r];
                return switch (c) {
                    case 0 -> z.pct() + " %";
                    case 1 -> propias.containsKey(z) ? propias.get(z) : delTipo(z);
                    default -> propias.containsKey(z) ? "propio" : "del tipo";
                };
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 1;
            }

            @Override
            public void setValueAt(Object v, int r, int c) {
                try {
                    int n = Integer.parseInt(v.toString().trim());
                    if (n < 1) return;
                    Zona z = Zona.values()[r];
                    if (n == delTipo(z)) propias.remove(z);
                    else propias.put(z, n);
                    fireTableRowsUpdated(r, r);
                } catch (NumberFormatException ignored) {
                    // valor inválido: se descarta
                }
            }
        };
        JTable t = new JTable(modelo);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foco, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tb, v, sel, foco, r, c);
                if (!sel) comp.setForeground(propias.containsKey(Zona.values()[r]) ? Color.BLACK : Color.GRAY);
                return comp;
            }
        });
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createTitledBorder("Repeticiones por serie (gris = heredado del tipo)"));
        sp.setPreferredSize(new java.awt.Dimension(320, 230));
        return sp;
    }

    private int delTipo(Zona z) {
        int[] v = ctx.metodologia().repsPorSerie().getOrDefault((Tipo) tipo.getSelectedItem(),
                ctx.metodologia().repsPorSerie().get(Tipo.FUERZA));
        return v[z.ordinal()];
    }

    private void guardar() {
        try {
            Double nuevoRm = numero(rm.getText(), "1RM");
            Double nuevoObj = numero(objetivo.getText(), "1RM objetivo");
            if (nombre.getText().isBlank()) throw new IllegalArgumentException("El nombre no puede quedar vacío.");
            ctx.ejercicios.guardar(new Ejercicio(original.id(), original.perfilId(), nombre.getText().trim(),
                    (Tipo) tipo.getSelectedItem(), nuevoRm, nuevoObj, (Integer) techo.getValue(), Map.copyOf(propias),
                    original.grupoPlio(), original.orden(), original.activo()));
            guardado = true;
            dispose();
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    private static Double numero(String s, String campo) {
        if (s == null || s.isBlank()) return null;
        try {
            double v = Double.parseDouble(s.trim().replace(',', '.'));
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + ": \"" + s + "\" no es un número válido.");
        }
    }

    private static void fila(JPanel p, int y, String etiqueta, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        p.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        p.add(campo, c);
    }
}
