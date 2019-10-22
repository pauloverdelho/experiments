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
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Notification.Type;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import javax.servlet.annotation.WebServlet;

/**
 * This UI is the application entry point. A UI may either represent a browser window
 * (or tab) or some part of an HTML page where a Vaadin application is embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is intended to be
 * overridden to add component to the user interface and initialize non-component functionality.
 */
@Theme("mytheme")
@Title("Conversor de documentos")
public class DocumentsConversionUI extends UI {

    private IntervalService intervalService = new IntervalService();
    private File tempFile;
    private TextArea documents;
    private TextArea facturas;
    private TextArea notasDeCredito;
    private Button clearDocuments;
    private Button copyFacturas;
    private Button copyNotasDeCredito;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout page = new HorizontalLayout();
        page.setSpacing(false);
        page.setMargin(true);
        page.setSizeFull();

        Panel resultPanel = new Panel("Converter documentos para facturas e notas de crédito");
        resultPanel.setSizeFull();

        GridLayout columns = new GridLayout(3, 1);
        columns.setWidth(100, Unit.PERCENTAGE);
        columns.setColumnExpandRatio(2, 1f);
        columns.setRowExpandRatio(0, 1f);
        columns.setSpacing(true);
        columns.setMargin(true);
        columns.setSizeFull();

        documents = new TextArea("Documentos");
        documents.setSizeFull();
        documents.setWidth(150, Unit.PIXELS);
        columns.addComponent(documents);

        Button convert = new Button(VaadinIcons.ANGLE_DOUBLE_RIGHT);
        convert.setWidth(100, Unit.PIXELS);
        convert.addClickListener(e -> calculateResults());
        convert.addStyleName("convert-button");

        copyFacturas = new Button(VaadinIcons.COPY_O);
        copyFacturas.setEnabled(false);
        copyFacturas.addStyleName("copy-button-facturas");

        copyNotasDeCredito = new Button(VaadinIcons.COPY_O);
        copyNotasDeCredito.setEnabled(false);
        copyNotasDeCredito.addStyleName("copy-button-notas");

        Label spacer1 = new Label();
        spacer1.setWidth(100, Unit.PIXELS);

        GridLayout lowerMiddle = new GridLayout(1, 4);
        lowerMiddle.setSpacing(true);
        lowerMiddle.setMargin(false);
        lowerMiddle.setSizeFull();
        lowerMiddle.addComponents(convert, copyFacturas, spacer1, copyNotasDeCredito);
        columns.addComponent(lowerMiddle);

        GridLayout rightSide = new GridLayout(1, 2);
        rightSide.setSpacing(true);
        rightSide.setMargin(false);
        rightSide.setSizeFull();

        facturas = new TextArea("Facturas");
        facturas.setSizeFull();
        facturas.setReadOnly(true);

        notasDeCredito = new TextArea("Notas de Crédito");
        notasDeCredito.setSizeFull();
        notasDeCredito.setReadOnly(true);

        rightSide.addComponents(facturas, notasDeCredito);
        columns.addComponent(rightSide);

        GridLayout documentsTools = new GridLayout(2, 1);

        Upload upload = createUpload();
        documentsTools.addComponent(upload);

        clearDocuments = getClearButton();
        documentsTools.addComponent(clearDocuments);

        columns.addComponent(documentsTools);

        Label spacer2 = new Label();
        spacer2.setWidth(100, Unit.PIXELS);
        columns.addComponent(spacer2);

        resultPanel.setContent(columns);
        page.addComponents(resultPanel);

        setContent(page);
        initializeClipboard();
    }

    private void initializeClipboard() {
        JSClipboard clipFacturas = new JSClipboard();
        clipFacturas.addSuccessListener((JSClipboard.SuccessListener) () -> Notification.show("Copiado!"));
        clipFacturas.addErrorListener((JSClipboard.ErrorListener) () -> Notification.show("Erro a copiar!", Notification.Type.ERROR_MESSAGE));
        clipFacturas.apply(copyFacturas, facturas);

        JSClipboard clipNotasDeCredito = new JSClipboard();
        clipNotasDeCredito.addSuccessListener((JSClipboard.SuccessListener) () -> Notification.show("Copiado!"));
        clipNotasDeCredito.addErrorListener((JSClipboard.ErrorListener) () -> Notification.show("Erro a copiar!", Notification.Type.ERROR_MESSAGE));
        clipNotasDeCredito.apply(copyNotasDeCredito, notasDeCredito);
    }

    private void calculateResults() {
        Map<IntervalType, String> intervals = intervalService.getIntervals(documents.getValue());
        facturas.setValue(intervals.getOrDefault(IntervalType.FACTURA, ""));
        notasDeCredito.setValue(intervals.getOrDefault(IntervalType.NOTA_DE_CREDITO, ""));
        copyFacturas.setEnabled(!facturas.getValue().isEmpty());
        copyNotasDeCredito.setEnabled(!notasDeCredito.getValue().isEmpty());
    }

    private Upload createUpload() {
        Upload upload = new Upload(null, (Upload.Receiver) (filename, mimeType) -> {
            try {
                tempFile = File.createTempFile("temp", ".csv");
                return new FileOutputStream(tempFile);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
        upload.addFinishedListener((Upload.FinishedListener) finishedEvent -> {
            try {
                /* Let's build a container from the CSV File */
                BufferedReader reader = new BufferedReader(new FileReader(tempFile));
                StringBuilder builder = new StringBuilder();
                reader.lines()
                        .filter(line -> line != null && line.trim().length() > 0)
                        .map(line -> line + "\n")
                        .forEach(builder::append);
                documents.setValue(builder.toString());
                calculateResults();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return upload;
    }

    private Button getClearButton() {
        Button clear = new Button(VaadinIcons.ERASER);
        clear.addClickListener(event -> documents.clear());
        clear.addStyleName("clear-button");
        return clear;
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = DocumentsConversionUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {

    }
}
