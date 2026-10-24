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
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    /** Período sombreado en un gráfico de fechas (por ejemplo, un mesociclo). */
    public record Franja(LocalDate desde, LocalDate hasta, String etiqueta) {
    }

    /** Volumen en barras e IMR en líneas (eje derecho): teoría (tablas) vs planilla (series del PDF). */
    public static JPanel volumenIntensidad(List<PuntoGrafico> plan, List<PuntoGrafico> real, String prefijo) {
        return volumenIntensidad(plan, real, prefijo, "teoría", "planilla");
    }

    /** Igual que el anterior, con los nombres de las dos series elegidos por quien llama. */
    public static JPanel volumenIntensidad(List<PuntoGrafico> a, List<PuntoGrafico> b, String prefijo,
                                           String nombreA, String nombreB) {
        DefaultCategoryDataset vol = new DefaultCategoryDataset();
        DefaultCategoryDataset imr = new DefaultCategoryDataset();
        agregar(vol, imr, a, nombreA, prefijo);
        agregar(vol, imr, b, nombreB, prefijo);
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

    /** 1RM de varios ejercicios en el tiempo, con su objetivo punteado y los mesociclos sombreados. */
    public static JPanel evolucionRmVarios(Map<String, List<PuntoRm>> series, Map<String, Double> objetivos,
                                           List<Franja> franjas) {
        TimeSeriesCollection ds = new TimeSeriesCollection();
        for (Map.Entry<String, List<PuntoRm>> en : series.entrySet()) {
            TimeSeries ts = new TimeSeries(en.getKey());
            // dos cambios el mismo día: queda el último
            for (PuntoRm p : en.getValue()) ts.addOrUpdate(new Day(fecha(p.fecha())), p.rm());
            ds.addSeries(ts);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Evolución del 1RM", "", "kg", ds, true, true, false);
        XYPlot plot = chart.getXYPlot();
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(false);
        ((DateAxis) plot.getDomainAxis()).setDateFormatOverride(new java.text.SimpleDateFormat("dd/MM/yy"));
        XYLineAndShapeRenderer r = (XYLineAndShapeRenderer) plot.getRenderer();
        r.setDefaultShapesVisible(true);
        int i = 0;
        for (Map.Entry<String, List<PuntoRm>> en : series.entrySet()) {
            Double obj = objetivos.get(en.getKey());
            if (obj != null) {
                ValueMarker m = new ValueMarker(obj, (Color) r.lookupSeriesPaint(i), new BasicStroke(1.2f,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f));
                m.setLabel("objetivo " + en.getKey());
                m.setLabelAnchor(RectangleAnchor.TOP_LEFT);
                m.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
                plot.addRangeMarker(m);
            }
            i++;
        }
        int k = 0;
        for (Franja f : franjas) {
            IntervalMarker m = new IntervalMarker(fecha(f.desde()).getTime(), fecha(f.hasta()).getTime(),
                    k++ % 2 == 0 ? new Color(120, 160, 220, 45) : new Color(120, 200, 140, 45));
            m.setLabel(f.etiqueta());
            m.setLabelAnchor(RectangleAnchor.TOP);
            m.setLabelTextAnchor(TextAnchor.TOP_CENTER);
            plot.addDomainMarker(m, Layer.BACKGROUND);
        }
        return panel(chart);
    }

    /** Barras agrupadas: una serie por entrada de {@code series}, con un valor por categoría. */
    public static JPanel barras(String titulo, String ejeY, List<String> categorias, Map<String, double[]> series) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (Map.Entry<String, double[]> en : series.entrySet()) {
            for (int i = 0; i < categorias.size(); i++) ds.addValue(en.getValue()[i], en.getKey(), categorias.get(i));
        }
        JFreeChart chart = ChartFactory.createBarChart(titulo, "", ejeY, ds, PlotOrientation.VERTICAL, true, true, false);
        chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(
                org.jfree.chart.axis.CategoryLabelPositions.UP_45);
        return panel(chart);
    }

    private static Date fecha(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
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
