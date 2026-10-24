package com.rutina.ui;

import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.PuntoRm;
import com.rutina.model.Tipo;
import com.rutina.service.Progreso;
import com.rutina.service.calculo.CalculadoraMesociclo;
import com.rutina.service.calculo.ResultadoMesociclo;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pestaña Progreso: tu recorrido a lo largo de los mesociclos (planilla vs entrenado). */
final class PanelProgreso extends JPanel {

    private static final DateTimeFormatter CORTA = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final String SIN_REGISTRO = "<html><div style='text-align:center'>Todavía no hay sesiones entrenadas.<br>"
            + "Este gráfico se completa con el registro de sesiones del celular (pendiente de programar).</div></html>";

    private final Contexto ctx;
    private final JPanel tarjetas = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final JTabbedPane graficos = new JTabbedPane();
    private final DefaultListModel<Ejercicio> modeloEjercicios = new DefaultListModel<>();
    private final JList<Ejercicio> listaEjercicios = new JList<>(modeloEjercicios);
    private final JPanel panelRm = new JPanel(new BorderLayout());
    private final JComboBox<String> agrupacion = new JComboBox<>(new String[]{"Por mesociclo", "Por semana"});
    private final JPanel panelTonelaje = new JPanel(new BorderLayout());

    // datos de la última carga
    private List<Mesociclo> mesociclos = List.of();
    private List<ResultadoMesociclo> resultados = List.of();
    private Map<Long, List<PuntoRm>> historial = Map.of();
    private int conError;
    private boolean cargando;

    PanelProgreso(Contexto ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        add(tarjetas, BorderLayout.NORTH);
        add(graficos, BorderLayout.CENTER);

        listaEjercicios.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listaEjercicios.setCellRenderer((l, e, i, sel, foco) -> {
            JLabel lb = new JLabel(e.nombre());
            lb.setOpaque(true);
            lb.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            lb.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
            lb.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
            return lb;
        });
        listaEjercicios.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !cargando) dibujarRm();
        });
        JScrollPane sp = new JScrollPane(listaEjercicios);
        sp.setBorder(BorderFactory.createTitledBorder("Ejercicios (Ctrl+click para varios)"));
        JSplitPane rm = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp, panelRm);
        rm.setDividerLocation(230);

        JPanel tonelaje = new JPanel(new BorderLayout());
        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
        arriba.add(new JLabel("Ver:"));
        arriba.add(agrupacion);
        arriba.add(new JLabel("  Tonelaje = repeticiones × kg de la planilla. Solo ejercicios con 1RM; hipertrofia no suma."));
        agrupacion.addActionListener(e -> dibujarTonelaje());
        tonelaje.add(arriba, BorderLayout.NORTH);
        tonelaje.add(panelTonelaje, BorderLayout.CENTER);

        graficos.addTab("1RM", rm);
        graficos.addTab("Volumen e intensidad", new JPanel(new BorderLayout()));
        graficos.addTab("Tonelaje", tonelaje);
        graficos.addTab("Cumplimiento", aviso(SIN_REGISTRO));
        graficos.addTab("Sensaciones", aviso(SIN_REGISTRO));

        ctx.alCambiar(this::recargar);
        recargar();
    }

    private void recargar() {
        try {
            mesociclos = ctx.mesociclos.listar(ctx.perfil.id()).stream().filter(Progreso::armado)
                    .sorted(Comparator.comparing(Mesociclo::inicio)).toList();
            Map<Long, Ejercicio> ejs = ctx.mapaEjercicios();
            List<Mesociclo> calculables = new ArrayList<>();
            List<ResultadoMesociclo> res = new ArrayList<>();
            for (Mesociclo m : mesociclos) {
                try {
                    res.add(CalculadoraMesociclo.calcular(ctx.metodologia(), m, ejs));
                    calculables.add(m);
                } catch (Exception ignored) {
                    // un mesociclo con parámetros inválidos se ve (con su error) en la pestaña Mesociclo
                }
            }
            conError = mesociclos.size() - calculables.size();
            mesociclos = calculables;
            resultados = res;
            Map<Long, List<PuntoRm>> h = new LinkedHashMap<>();
            for (Ejercicio e : ejs.values()) {
                if (e.tipo() == Tipo.PLIOMETRIA) continue;
                List<PuntoRm> puntos = ctx.ejercicios.historial(e.id());
                if (!puntos.isEmpty()) h.put(e.id(), puntos);
            }
            historial = h;
            dibujarTarjetas(ejs);
            cargarListaEjercicios(ejs);
            dibujarRm();
            dibujarVolumen();
            dibujarTonelaje();
        } catch (Exception e) {
            Contexto.error(this, e);
        }
    }

    // ---- tarjetas --------------------------------------------------------------------------------------------

    private void dibujarTarjetas(Map<Long, Ejercicio> ejs) {
        tarjetas.removeAll();
        int semanas = mesociclos.stream().mapToInt(Mesociclo::microciclos).sum();
        tarjetas.add(tarjeta("Mesociclos armados", mesociclos.size() + (conError > 0 ? "  (+" + conError + " con error, no se muestran)" : "")));
        tarjetas.add(tarjeta("Semanas planificadas", String.valueOf(semanas)));
        tarjetas.add(tarjeta("Sesiones entrenadas", "—"));
        List<Progreso.Mejora> mejoras = Progreso.mejoras(new ArrayList<>(ejs.values()), historial);
        for (Progreso.Mejora m : mejoras.subList(0, Math.min(4, mejoras.size()))) {
            tarjetas.add(tarjeta(m.nombre(), kg(m.inicial()) + " → " + kg(m.actual()) + " kg  ("
                    + (m.pct() >= 0 ? "+" : "") + String.format("%.1f", m.pct()).replace('.', ',') + " %)"));
        }
        if (mejoras.isEmpty()) tarjetas.add(tarjeta("Mejoras de 1RM", "cuando actualices algún 1RM"));
        tarjetas.revalidate();
        tarjetas.repaint();
    }

    private static JPanel tarjeta(String titulo, String valor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        JLabel t = new JLabel(titulo);
        t.setForeground(Color.GRAY);
        JLabel v = new JLabel(valor);
        v.setFont(v.getFont().deriveFont(Font.BOLD, v.getFont().getSize2D() + 3));
        p.add(t, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    // ---- 1RM -------------------------------------------------------------------------------------------------

    private void cargarListaEjercicios(Map<Long, Ejercicio> ejs) {
        List<Long> elegidos = listaEjercicios.getSelectedValuesList().stream().map(Ejercicio::id).toList();
        cargando = true;
        modeloEjercicios.clear();
        List<Ejercicio> conHistorial = ejs.values().stream().filter(e -> historial.containsKey(e.id())).toList();
        conHistorial.forEach(modeloEjercicios::addElement);
        // por defecto, los tres que más mejoraron
        List<String> top = Progreso.mejoras(conHistorial, historial).stream().limit(3).map(Progreso.Mejora::nombre).toList();
        List<Integer> sel = new ArrayList<>();
        for (int i = 0; i < conHistorial.size(); i++) {
            Ejercicio e = conHistorial.get(i);
            if (elegidos.isEmpty() ? top.contains(e.nombre()) : elegidos.contains(e.id())) sel.add(i);
        }
        listaEjercicios.setSelectedIndices(sel.stream().mapToInt(Integer::intValue).toArray());
        cargando = false;
    }

    private void dibujarRm() {
        panelRm.removeAll();
        List<Ejercicio> sel = listaEjercicios.getSelectedValuesList();
        if (sel.isEmpty()) {
            panelRm.add(aviso("Elegí uno o más ejercicios de la lista."), BorderLayout.CENTER);
        } else {
            Map<String, List<PuntoRm>> series = new LinkedHashMap<>();
            Map<String, Double> objetivos = new LinkedHashMap<>();
            for (Ejercicio e : sel) {
                series.put(e.nombre(), historial.get(e.id()));
                if (e.rmObjetivo() != null) objetivos.put(e.nombre(), e.rmObjetivo());
            }
            List<Graficos.Franja> franjas = new ArrayList<>();
            for (int i = 0; i < mesociclos.size(); i++) {
                Mesociclo m = mesociclos.get(i);
                franjas.add(new Graficos.Franja(m.inicio(), m.fin(), "M" + (i + 1)));
            }
            panelRm.add(Graficos.evolucionRmVarios(series, objetivos, franjas), BorderLayout.CENTER);
        }
        panelRm.revalidate();
        panelRm.repaint();
    }

    // ---- volumen e intensidad ------------------------------------------------------------------------------

    private void dibujarVolumen() {
        JPanel p = new JPanel(new BorderLayout());
        if (resultados.isEmpty()) {
            p.add(aviso("Todavía no hay mesociclos armados."), BorderLayout.CENTER);
        } else {
            p.add(Graficos.volumenIntensidad(Progreso.planillaPorMesociclo(resultados), List.of(), "M",
                    "planilla", "entrenado"), BorderLayout.CENTER);
            p.add(new JLabel("  M1, M2…: mesociclos en orden (" + leyendaMesociclos()
                    + "). La serie \"entrenado\" aparece cuando importes el registro del celular."), BorderLayout.SOUTH);
        }
        graficos.setComponentAt(1, p);
    }

    // ---- tonelaje --------------------------------------------------------------------------------------------

    private void dibujarTonelaje() {
        panelTonelaje.removeAll();
        if (resultados.isEmpty()) {
            panelTonelaje.add(aviso("Todavía no hay mesociclos armados."), BorderLayout.CENTER);
        } else {
            boolean porSemana = agrupacion.getSelectedIndex() == 1;
            List<String> categorias = new ArrayList<>();
            List<Double> valores = new ArrayList<>();
            for (int i = 0; i < mesociclos.size(); i++) {
                double[] t = Progreso.tonelajePorMicro(mesociclos.get(i), resultados.get(i), ctx.metodologia());
                if (porSemana) {
                    for (int w = 0; w < t.length; w++) {
                        categorias.add("M" + (i + 1) + "·S" + (w + 1));
                        valores.add(t[w]);
                    }
                } else {
                    categorias.add("M" + (i + 1) + " " + mesociclos.get(i).inicio().format(CORTA));
                    valores.add(java.util.Arrays.stream(t).sum());
                }
            }
            Map<String, double[]> series = new LinkedHashMap<>();
            series.put("Planilla", valores.stream().mapToDouble(Double::doubleValue).toArray());
            panelTonelaje.add(Graficos.barras("Tonelaje", "kg", categorias, series), BorderLayout.CENTER);
        }
        panelTonelaje.revalidate();
        panelTonelaje.repaint();
    }

    // ---- utilidades ------------------------------------------------------------------------------------------

    private String leyendaMesociclos() {
        List<String> partes = new ArrayList<>();
        for (int i = 0; i < mesociclos.size(); i++) partes.add("M" + (i + 1) + " = " + mesociclos.get(i).inicio().format(CORTA));
        return String.join(", ", partes);
    }

    private static JPanel aviso(String texto) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel(texto, SwingConstants.CENTER);
        l.setForeground(Color.GRAY);
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private static String kg(double v) {
        return v % 1 == 0 ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',');
    }
}
