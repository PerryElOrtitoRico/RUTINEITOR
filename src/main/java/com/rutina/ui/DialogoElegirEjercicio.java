package com.rutina.ui;

import com.rutina.model.Ejercicio;
import com.rutina.model.Tipo;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.List;

/** Lista de ejercicios activos agrupada por tipo, con buscador. */
final class DialogoElegirEjercicio {

    private DialogoElegirEjercicio() {
    }

    /** null si se canceló. */
    static Ejercicio elegir(Component padre, List<Ejercicio> ejercicios) {
        List<Ejercicio> candidatos = ejercicios.stream()
                .filter(e -> e.activo() && e.tipo() != Tipo.PLIOMETRIA)
                .sorted(Comparator.comparing(Ejercicio::tipo).thenComparing(Ejercicio::orden))
                .toList();
        DefaultListModel<Ejercicio> modelo = new DefaultListModel<>();
        JList<Ejercicio> lista = new JList<>(modelo);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer((l, e, i, sel, foco) -> {
            JLabel lb = new JLabel("[" + PanelEjercicios.titulo(e.tipo()) + "]  " + e.nombre());
            lb.setOpaque(true);
            lb.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            lb.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
            lb.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
            return lb;
        });
        JTextField buscar = new JTextField();
        Runnable filtrar = () -> {
            String q = buscar.getText().trim().toLowerCase();
            modelo.clear();
            candidatos.stream().filter(e -> e.nombre().toLowerCase().contains(q)).forEach(modelo::addElement);
            if (!modelo.isEmpty()) lista.setSelectedIndex(0);
        };
        buscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filtrar.run();
            }

            public void removeUpdate(DocumentEvent e) {
                filtrar.run();
            }

            public void changedUpdate(DocumentEvent e) {
                filtrar.run();
            }
        });
        filtrar.run();

        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.add(buscar, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(lista);
        sp.setPreferredSize(new Dimension(380, 360));
        p.add(sp, BorderLayout.CENTER);
        int r = JOptionPane.showConfirmDialog(padre, p, "Elegir ejercicio", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        return r == JOptionPane.OK_OPTION ? lista.getSelectedValue() : null;
    }
}
