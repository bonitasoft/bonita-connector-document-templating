/**
 * Copyright (C) 2015 Bonitasoft S.A.
 * Bonitasoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * Bonitasoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or Bonitasoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 **/

package com.bonitasoft.connectors.document.templating;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
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

    private static final String TEMP_DOC = "connectorDocumentTemplatingTempDocument";
    private static final String TEMP_DIR = "connectorDocumentTemplatingTempDirectory";
    private static final String ODT_EXT = ".odt";
    private static final String DOCX_EXT = ".docx";

    public static final String INPUT_DOCUMENT_INPUT = "documentInput";
    public static final String INPUT_REPLACEMENTS = "replacements";
    public static final String INPUT_RESULTING_DOC_FILENAME = "outputFileName";
    public static final String OUTPUT_DOCUMENT = "document";

    private Logger logger = Logger.getLogger(DocumentTemplating.class.getName());
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
            Document document = retrieveDocument();
            String outputFilename = (String) getInputParameter(INPUT_RESULTING_DOC_FILENAME);
            boolean isOdt = document.getContentFileName().endsWith(ODT_EXT);
            byte[] content = getAPIAccessor().getProcessAPI().getDocumentContent(document.getContentStorageId());
            List<List<Object>> replacements = (List<List<Object>>) getInputParameter(INPUT_REPLACEMENTS);

            byte[] finalDocument = applyReplacements(content, replacements, isOdt);
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
        try (InputStream is = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));) {
            Path targetDir = ZipUtil.unzip(TEMP_DIR, zis);
            Path documentPath = retrieveDocumentPath(isOdt, targetDir);
            if (isCorrupted(documentPath)) {
                logger.warning(String.format(
                        "Invalid XML characters have been detected in the document `%s`, they will be removed.",
                        getInputParameter(INPUT_DOCUMENT_INPUT)));
                sanitizeFile(documentPath);
            }
            Path tempResFile = Files.createTempFile(TEMP_DOC, isOdt ? ODT_EXT : DOCX_EXT);
            ZipUtil.zip(targetDir, tempResFile);
            return tempResFile.toFile();
        }
    }

    protected boolean isCorrupted(Path filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            CharBuffer buffer = CharBuffer.allocate(ZipUtil.BUFFER_SIZE);
            while (reader.read(buffer) != -1) {
                buffer.flip();
                String currentString = buffer.toString();
                if (!Objects.equals(currentString, lookupTranslator.translate(currentString))) {
                    return true;
                }
                buffer.clear();
            }
        }
        return false;
    }

    private Path retrieveDocumentPath(boolean isOdt, Path targetDir) {
        return isOdt
                ? targetDir.resolve("content.xml")
                : targetDir.resolve("word").resolve("document.xml");
    }

    private void sanitizeFile(Path filePathToSanitize) throws IOException {
        File fileToSanitize = filePathToSanitize.toFile();
        Path tempFile = Files.createTempFile(fileToSanitize.getName(), null);
        Files.copy(filePathToSanitize, tempFile, StandardCopyOption.REPLACE_EXISTING);
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile.toFile()));
                FileWriter writer = new FileWriter(fileToSanitize)) {
            CharBuffer buffer = CharBuffer.allocate(ZipUtil.BUFFER_SIZE);
            while (reader.read(buffer) != -1) {
                buffer.flip();
                lookupTranslator.translate(buffer.toString(), writer);
                buffer.clear();
            }
        }
    }

    private DocumentValue createDocumentValue(Document document, String outputFilename, byte[] content) {
        return new DocumentValue(content, document.getContentMimeType(),
                outputFilename != null ? outputFilename : document.getContentFileName());
    }

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            Document document = retrieveDocument();
            if (!(document.getContentFileName().endsWith(DOCX_EXT) || document.getContentFileName().endsWith(ODT_EXT))) {
                throw new ConnectorValidationException(
                        "The template must be a .docx or a .odt document, other formats are not supported.");
            }
        } catch (DocumentNotFoundException e) {
            throw new ConnectorValidationException(e.getMessage());
        }
    }

    private Document retrieveDocument() throws DocumentNotFoundException {
        ProcessAPI processAPI = getAPIAccessor().getProcessAPI();
        long processInstanceId = getExecutionContext().getProcessInstanceId();
        return processAPI.getLastDocument(processInstanceId,
                (String) getInputParameter(INPUT_DOCUMENT_INPUT));
    }
}
