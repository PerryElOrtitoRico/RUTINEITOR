package com.rutina.ui;

import com.rutina.model.PuntoRm;
import com.rutina.service.calculo.ResultadoMesociclo.PuntoGrafico;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Gráficos de la aplicación (JFreeChart). */
public final class Graficos {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yy");

    private Graficos() {
    }

    /** Evolución del 1RM con el objetivo como línea de referencia. */
    public static JPanel evolucionRm(List<PuntoRm> puntos, Double objetivo) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (int i = 0; i < puntos.size(); i++) {
            // la clave incluye el índice para que dos cambios el mismo día no se pisen
            String clave = puntos.get(i).fecha().format(FECHA) + (i > 0 && puntos.get(i).fecha().equals(puntos.get(i - 1).fecha()) ? " (" + i + ")" : "");
            ds.addValue(puntos.get(i).rm(), "1RM", clave);
        }
        JFreeChart chart = ChartFactory.createLineChart("Evolución del 1RM", "Fecha", "kg", ds,
                PlotOrientation.VERTICAL, false, true, false);
        CategoryPlot plot = chart.getCategoryPlot();
        ((LineAndShapeRenderer) plot.getRenderer()).setDefaultShapesVisible(true);
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(false);
        if (objetivo != null) {
            ValueMarker m = new ValueMarker(objetivo, new Color(0, 140, 0), new BasicStroke(1.5f));
            m.setLabel("objetivo " + objetivo);
            plot.addRangeMarker(m);
        }
        return panel(chart);
    }

    /** Volumen en barras e IMR en líneas (eje derecho), planificado vs real. */
    public static JPanel volumenIntensidad(List<PuntoGrafico> plan, List<PuntoGrafico> real, String prefijo) {
        DefaultCategoryDataset vol = new DefaultCategoryDataset();
        DefaultCategoryDataset imr = new DefaultCategoryDataset();
        agregar(vol, imr, plan, "plan", prefijo);
        agregar(vol, imr, real, "real", prefijo);
        JFreeChart chart = ChartFactory.createBarChart("Volumen e intensidad", "", "Repeticiones", vol,
                PlotOrientation.VERTICAL, true, true, false);
        CategoryPlot plot = chart.getCategoryPlot();
        NumberAxis eje = new NumberAxis("IMR %");
        eje.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, eje);
        plot.setDataset(1, imr);
        plot.mapDatasetToRangeAxis(1, 1);
        LineAndShapeRenderer lineas = new LineAndShapeRenderer();
        lineas.setDefaultShapesVisible(true);
        plot.setRenderer(1, lineas);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        return panel(chart);
    }

    private static void agregar(DefaultCategoryDataset vol, DefaultCategoryDataset imr, List<PuntoGrafico> puntos,
                                String cual, String prefijo) {
        for (PuntoGrafico p : puntos) {
            String serie = nombre(p) + " " + cual;
            String cat = prefijo + p.microciclo();
            vol.addValue(p.volumen(), serie, cat);
            if (p.volumen() > 0) imr.addValue(p.imr(), "IMR " + serie, cat);
        }
    }

    private static String nombre(PuntoGrafico p) {
        String s = p.tipo().name().toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static JPanel panel(JFreeChart chart) {
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(520, 300));
        return cp;
    }
}
