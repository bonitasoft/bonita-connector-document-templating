/**
 * Bonitasoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * Copyright (C) 2015 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or Bonitasoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 **/

package com.bonitasoft.connectors.document.templating;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.translate.LookupTranslator;
import org.apache.velocity.tools.generic.SortTool;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.document.DocumentNotFoundException;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;

/**
 * @author Baptiste Mesta
 */
public class DocumentTemplating extends AbstractConnector {

    public static String INPUT_DOCUMENT_INPUT = "documentInput";
    public static String INPUT_REPLACEMENTS = "replacements";
    public static String INPUT_RESULTING_DOC_FILENAME = "outputFileName";

    public static String OUTPUT_DOCUMENT = "document";
    private LookupTranslator lookupTranslator;

    public DocumentTemplating() {
        Map<CharSequence, CharSequence> escapeXml10Map = new HashMap<>();
        escapeXml10Map.put("\u0000", StringUtils.EMPTY);
        escapeXml10Map.put("\u0001", StringUtils.EMPTY);
        escapeXml10Map.put("\u0002", StringUtils.EMPTY);
        escapeXml10Map.put("\u0003", StringUtils.EMPTY);
        escapeXml10Map.put("\u0004", StringUtils.EMPTY);
        escapeXml10Map.put("\u0005", StringUtils.EMPTY);
        escapeXml10Map.put("\u0006", StringUtils.EMPTY);
        escapeXml10Map.put("\u0007", StringUtils.EMPTY);
        escapeXml10Map.put("\u0008", StringUtils.EMPTY);
        escapeXml10Map.put("\u000b", StringUtils.EMPTY);
        escapeXml10Map.put("\u000c", StringUtils.EMPTY);
        escapeXml10Map.put("\u000e", StringUtils.EMPTY);
        escapeXml10Map.put("\u000f", StringUtils.EMPTY);
        escapeXml10Map.put("\u0010", StringUtils.EMPTY);
        escapeXml10Map.put("\u0011", StringUtils.EMPTY);
        escapeXml10Map.put("\u0012", StringUtils.EMPTY);
        escapeXml10Map.put("\u0013", StringUtils.EMPTY);
        escapeXml10Map.put("\u0014", StringUtils.EMPTY);
        escapeXml10Map.put("\u0015", StringUtils.EMPTY);
        escapeXml10Map.put("\u0016", StringUtils.EMPTY);
        escapeXml10Map.put("\u0017", StringUtils.EMPTY);
        escapeXml10Map.put("\u0018", StringUtils.EMPTY);
        escapeXml10Map.put("\u0019", StringUtils.EMPTY);
        escapeXml10Map.put("\u001a", StringUtils.EMPTY);
        escapeXml10Map.put("\u001b", StringUtils.EMPTY);
        escapeXml10Map.put("\u001c", StringUtils.EMPTY);
        escapeXml10Map.put("\u001d", StringUtils.EMPTY);
        escapeXml10Map.put("\u001e", StringUtils.EMPTY);
        escapeXml10Map.put("\u001f", StringUtils.EMPTY);
        escapeXml10Map.put("\ufffe", StringUtils.EMPTY);
        escapeXml10Map.put("\uffff", StringUtils.EMPTY);
        lookupTranslator = new LookupTranslator(Collections.unmodifiableMap(escapeXml10Map));
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            final ProcessAPI processAPI = getAPIAccessor().getProcessAPI();
            final long processInstanceId = getExecutionContext().getProcessInstanceId();
            final Document document = processAPI.getLastDocument(processInstanceId,
                    (String) getInputParameter(INPUT_DOCUMENT_INPUT));
            final String outputFilename = (String) getInputParameter(INPUT_RESULTING_DOC_FILENAME);
            boolean isOdt = document.getContentFileName().endsWith(".odt");
            final byte[] content = processAPI.getDocumentContent(document.getContentStorageId());

            final byte[] finalDocument = applyReplacements(content,
                    (List<List<Object>>) getInputParameter(INPUT_REPLACEMENTS), isOdt);

            setOutputParameter(OUTPUT_DOCUMENT, createDocumentValue(document, outputFilename, finalDocument));
        } catch (final DocumentNotFoundException e) {
            throw new ConnectorException(e);
        }
    }

    protected byte[] applyReplacements(byte[] content, List<List<Object>> inputParameter, boolean isOdt)
            throws ConnectorException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(content)) {
            IXDocReport report = XDocReportRegistry.getRegistry().loadReport(is, TemplateEngineKind.Velocity);
            IContext context = report.createContext();
            context.put("sorter", new SortTool());
            for (List<Object> objects : inputParameter) {
                if (objects != null && objects.size() > 1) {
                    context.put(String.valueOf(objects.get(0)), objects.get(1));
                }
            }

            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                report.process(context, byteArrayOutputStream);
                File resFile = sanitizeOutput(byteArrayOutputStream, isOdt);
                try (FileInputStream fis = new FileInputStream(resFile)) {
                    return IOUtils.toByteArray(fis);
                }

            }
        } catch (final IOException | XDocReportException e) {
            throw new ConnectorException(e);
        }
    }

    private File sanitizeOutput(ByteArrayOutputStream byteArrayOutputStream, boolean isOdt) throws IOException {
        String tempDir = "templateUnzipTempDir";
        try (InputStream is = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));) {
            Path targetDir = ZipUtil.unzip(tempDir, zis);
            Path filePathToSanitize = isOdt
                    ? targetDir.resolve("content.xml")
                    : targetDir.resolve("word").resolve("document.xml");
            byte[] contentToSanitize = Files.readAllBytes(filePathToSanitize);
            String contentSanitized = sanitize(new String(contentToSanitize));
            try (FileWriter fileWriter = new FileWriter(filePathToSanitize.toFile())) {
                fileWriter.write(contentSanitized);
            }
            Path tempResFile = Files.createTempFile("tempDoc", isOdt ? ".odt" : ".docx");
            ZipUtil.zip(targetDir, tempResFile);
            return tempResFile.toFile();
        }

    }

    private String sanitize(String stringToSanitize) {
        return lookupTranslator.translate(stringToSanitize);
    }

    private DocumentValue createDocumentValue(Document document, String outputFilename, byte[] content) {
        return new DocumentValue(content, document.getContentMimeType(),
                outputFilename != null ? outputFilename : document.getContentFileName());
    }

    @Override
    public void validateInputParameters() throws ConnectorValidationException {

    }
}
