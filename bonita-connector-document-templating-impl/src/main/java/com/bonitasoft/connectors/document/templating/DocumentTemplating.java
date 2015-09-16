/**
 * Copyright (C) 2015 Bonitasoft S.A.
 * Bonitasoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * Bonitasoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or Bonitasoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 **/

package com.bonitasoft.connectors.document.templating;

import java.util.Map;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.document.DocumentNotFoundException;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

/**
 * @author Baptiste Mesta
 */
public class DocumentTemplating extends AbstractConnector {

    public static String INPUT_DOCUMENT_INPUT = "documentInput";
    public static String INPUT_REPLACEMENTS = "replacements";

    public static String OUTPUT_DOCUMENT = "document";

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            ProcessAPI processAPI = getAPIAccessor().getProcessAPI();
            long processInstanceId = getExecutionContext().getProcessInstanceId();
            Document document = processAPI.getLastDocument(processInstanceId, (String) getInputParameter(INPUT_DOCUMENT_INPUT));
            byte[] content = processAPI.getDocumentContent(document.getContentStorageId());

            byte[] finalDocument = applyReplacements(content,(Map<String,String>)getInputParameter(INPUT_REPLACEMENTS));

            setOutputParameter(OUTPUT_DOCUMENT, createDocumentValue(document, finalDocument));
        } catch (DocumentNotFoundException e) {
            throw new ConnectorException(e);
        }
    }

    byte[] applyReplacements(byte[] content, Map<String, String> inputParameter) {
        return new byte[0];
    }

    private DocumentValue createDocumentValue(Document document, byte[] content) {
        return new DocumentValue(content, document.getContentMimeType(), document.getContentFileName());
    }

    @Override
    public void validateInputParameters() throws ConnectorValidationException {

    }
}
