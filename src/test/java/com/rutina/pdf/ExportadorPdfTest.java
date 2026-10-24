package com.rutina.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.rutina.model.Aproximacion;
import com.rutina.model.Bloque;
import com.rutina.model.Ejercicio;
import com.rutina.model.Mesociclo;
import com.rutina.model.Metodologia;
import com.rutina.model.ParametrosTipo;
import com.rutina.model.PlanPliometria;
import com.rutina.model.Tipo;
import com.rutina.service.calculo.CalculadoraMesociclo;
import com.rutina.service.calculo.Pliometria;
import com.rutina.service.datos.DatosIniciales;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExportadorPdfTest {

    private static final Metodologia M = DatosIniciales.metodologia();

    @TempDir
    Path dir;

    private Path pdf() {
        return dir.resolve("rutina.pdf");
    }

    private void exportar(Double rm, List<PlanConDias> planes) throws Exception {
        var ej = new Ejercicio(1, 1, "Sentadilla", Tipo.FUERZA, rm, null, 100, Map.of(), null, 0, true);
        var mes = new Mesociclo(0, 1, LocalDate.of(2026, 11, 2), 2, new ParametrosTipo(0, 72),
                new ParametrosTipo(200, 70), 'A', 3, Map.of(1L, Aproximacion.COMPLEJA),
                List.of(new Bloque(1, 1, 1L, 100)), "Lumbares en banco", "Abdominales", Map.of());
        var res = CalculadoraMesociclo.calcular(M, mes, Map.of(1L, ej));
        ExportadorPdf.exportar(pdf(), "Franco", mes, res, planes, M);
    }

    private PlanConDias planNivel1() {
        return new PlanConDias(new PlanPliometria(1, 1, 1, LocalDate.of(2026, 10, 26)),
                Pliometria.generar(M, 1, DatosIniciales.ejercicios()), Set.of(1, 2));
    }

    private int paginas() throws Exception {
        try (var r = new PdfReader(pdf().toString())) {
            return r.getNumberOfPages();
        }
    }

    /** Texto de la página sin espacios ni saltos (las celdas angostas parten las líneas). */
    private String texto(int pagina) throws Exception {
        try (var r = new PdfReader(pdf().toString())) {
            return new PdfTextExtractor(r).getTextFromPage(pagina).replaceAll("\\s+", "");
        }
    }

    @Test
    void unaPaginaSinPlio() throws Exception {
        exportar(100.0, List.of());
        assertEquals(1, paginas());
        assertTrue(texto(1).contains("80-87,5"), texto(1)); // redondeo de a 2,5 kg
        assertFalse(texto(1).contains("kg"), texto(1));     // los pesos van sin "kg"
        assertTrue(texto(1).contains("Sentadilla"));
        assertTrue(texto(1).contains("Lumbaresenbanco"));
    }

    @Test
    void filaAgrupadaConcatena() throws Exception {
        exportar(100.0, List.of());
        assertTrue(texto(1).contains("3.3.3.3.3.3.3.3.3.3.3"), texto(1)); // 80 % (6×3) + 85 % (5×3)
    }

    @Test
    void paginaPliometria() throws Exception {
        exportar(100.0, List.of(planNivel1()));
        assertEquals(2, paginas());
        assertTrue(texto(2).contains("4x10"), texto(2));
        assertTrue(texto(2).contains("Saltoconsoga"));
    }

    @Test
    void sinRmUsaPorcentaje() throws Exception {
        exportar(null, List.of());
        assertTrue(texto(1).contains("80-87,5%"), texto(1));
    }
}
