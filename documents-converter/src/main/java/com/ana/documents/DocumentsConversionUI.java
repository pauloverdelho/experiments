package com.ana.documents;

import com.ana.documents.service.IntervalService;
import com.ana.documents.service.IntervalType;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.jsclipboard.JSClipboard;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;

import javax.servlet.annotation.WebServlet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

@Theme("mytheme")
@Title("Conversor de documentos")
public class DocumentsConversionUI extends UI {

    private IntervalService intervalService = new IntervalService();
    private File tempFile;
    private TextArea documents;
    private TextArea facturas;
    private TextArea notasDeCredito;
    private Button copyFacturas;
    private Button copyNotasDeCredito;

    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout page = new HorizontalLayout();
        page.setSpacing(false);
        page.setMargin(true);
        page.setSizeFull();
        Panel resultPanel = new Panel("Converter documentos para facturas e notas de crédito");
        resultPanel.setSizeFull();
        GridLayout columns = new GridLayout(3, 1);
        columns.setWidth(100.0F, Unit.PERCENTAGE);
        columns.setColumnExpandRatio(2, 1.0F);
        columns.setRowExpandRatio(0, 1.0F);
        columns.setSpacing(true);
        columns.setMargin(true);
        columns.setSizeFull();
        this.documents = new TextArea("Documentos");
        this.documents.setSizeFull();
        this.documents.setWidth(150.0F, Unit.PIXELS);
        columns.addComponent(this.documents);
        Button convert = new Button(VaadinIcons.ANGLE_DOUBLE_RIGHT);
        convert.setWidth(100.0F, Unit.PIXELS);
        convert.addClickListener((e) -> {
            this.calculateResults();
        });
        convert.addStyleName("convert-button");
        this.copyFacturas = new Button(VaadinIcons.COPY_O);
        this.copyFacturas.setEnabled(false);
        this.copyFacturas.addStyleName("copy-button-facturas");
        this.copyNotasDeCredito = new Button(VaadinIcons.COPY_O);
        this.copyNotasDeCredito.setEnabled(false);
        this.copyNotasDeCredito.addStyleName("copy-button-notas");
        Label spacer1 = new Label();
        spacer1.setWidth(100.0F, Unit.PIXELS);
        GridLayout lowerMiddle = new GridLayout(1, 4);
        lowerMiddle.setSpacing(true);
        lowerMiddle.setMargin(false);
        lowerMiddle.setSizeFull();
        lowerMiddle.addComponents(new Component[] {convert, this.copyFacturas, spacer1, this.copyNotasDeCredito});
        columns.addComponent(lowerMiddle);
        GridLayout lowerRight = new GridLayout(1, 2);
        lowerRight.setSpacing(true);
        lowerRight.setMargin(false);
        lowerRight.setSizeFull();
        this.facturas = new TextArea("Facturas");
        this.facturas.setSizeFull();
        this.facturas.setReadOnly(true);
        this.notasDeCredito = new TextArea("Notas de Crédito");
        this.notasDeCredito.setSizeFull();
        this.notasDeCredito.setReadOnly(true);
        lowerRight.addComponents(new Component[] {this.facturas, this.notasDeCredito});
        columns.addComponent(lowerRight);
        Upload upload = this.createUpload();
        columns.addComponent(upload);
        Label spacer2 = new Label();
        spacer2.setWidth(100.0F, Unit.PIXELS);
        columns.addComponent(spacer2);
        resultPanel.setContent(columns);
        page.addComponents(new Component[] {resultPanel});
        this.setContent(page);
        this.initializeClipboard();
    }

    private void initializeClipboard() {
        JSClipboard clipFacturas = new JSClipboard();
        clipFacturas.addSuccessListener(() -> {
            Notification.show("Copiado!");
        });
        clipFacturas.addErrorListener(() -> {
            Notification.show("Erro a copiar!", Type.ERROR_MESSAGE);
        });
        clipFacturas.apply(this.copyFacturas, this.facturas);
        JSClipboard clipNotasDeCredito = new JSClipboard();
        clipNotasDeCredito.addSuccessListener(() -> {
            Notification.show("Copiado!");
        });
        clipNotasDeCredito.addErrorListener(() -> {
            Notification.show("Erro a copiar!", Type.ERROR_MESSAGE);
        });
        clipNotasDeCredito.apply(this.copyNotasDeCredito, this.facturas);
    }

    private void calculateResults() {
        Map<IntervalType, String> intervals = this.intervalService.getIntervals(this.documents.getValue());
        this.facturas.setValue((String) intervals.getOrDefault(IntervalType.FACTURA, ""));
        this.notasDeCredito.setValue((String) intervals.getOrDefault(IntervalType.NOTA_DE_CREDITO, ""));
        this.copyFacturas.setEnabled(!this.facturas.getValue().isEmpty());
        this.copyNotasDeCredito.setEnabled(!this.notasDeCredito.getValue().isEmpty());
    }

    private Upload createUpload() {
        Upload upload = new Upload((String) null, (filename, mimeType) -> {
            try {
                this.tempFile = File.createTempFile("temp", ".csv");
                return new FileOutputStream(this.tempFile);
            } catch (IOException var4) {
                var4.printStackTrace();
                return null;
            }
        });
        upload.addFinishedListener((finishedEvent) -> {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(this.tempFile));
                StringBuilder builder = new StringBuilder();
                reader.lines()
                        .filter(line -> line != null && line.trim().length() > 0)
                        .map(line -> line + "\n")
                        .forEach(builder::append);
                this.documents.setValue(builder.toString());
                this.calculateResults();
            } catch (IOException var4) {
                var4.printStackTrace();
            }

        });
        return upload;
    }

    @WebServlet(
            urlPatterns = {"/*"},
            name = "MyUIServlet",
            asyncSupported = true
    )
    @VaadinServletConfiguration(
            ui = DocumentsConversionUI.class,
            productionMode = false
    )
    public static class MyUIServlet extends VaadinServlet {
    }
}
