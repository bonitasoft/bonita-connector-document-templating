/**
 * Copyright (C) 2015 Bonitasoft S.A.
 * Bonitasoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * Bonitasoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or Bonitasoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 **/

package com.bonitasoft.connectors.document.templating;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

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

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            final ProcessAPI processAPI = getAPIAccessor().getProcessAPI();
            final long processInstanceId = getExecutionContext().getProcessInstanceId();
            final Document document = processAPI.getLastDocument(processInstanceId, (String) getInputParameter(INPUT_DOCUMENT_INPUT));
            final String outputFilename = (String) getInputParameter(INPUT_RESULTING_DOC_FILENAME);
            final byte[] content = processAPI.getDocumentContent(document.getContentStorageId());

            final byte[] finalDocument = applyReplacements(content, (List<List<Object>>) getInputParameter(INPUT_REPLACEMENTS));

            setOutputParameter(OUTPUT_DOCUMENT, createDocumentValue(document, outputFilename, finalDocument));
        } catch (final DocumentNotFoundException e) {
            throw new ConnectorException(e);
        }
    }

    byte[] applyReplacements(byte[] content, List<List<Object>> inputParameter) throws ConnectorException {
        try {
            final IXDocReport report = XDocReportRegistry.getRegistry().loadReport(new ByteArrayInputStream(content), TemplateEngineKind.Velocity);

            final IContext context = report.createContext();
            for (final List<Object> objects : inputParameter) {
                if (objects != null && objects.size() > 1) {
                    context.put(String.valueOf(objects.get(0)), objects.get(1));
                }
            }

            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            report.process(context, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (final IOException | XDocReportException e) {
            throw new ConnectorException(e);
        }
    }

    private DocumentValue createDocumentValue(Document document, String outputFilename, byte[] content) {
        return new DocumentValue(content, document.getContentMimeType(), outputFilename != null ? outputFilename : document.getContentFileName());
    }

    @Override
    public void validateInputParameters() throws ConnectorValidationException {

    }
}
