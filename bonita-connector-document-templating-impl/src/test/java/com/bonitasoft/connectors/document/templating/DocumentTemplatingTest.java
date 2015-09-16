/**
 * Copyright (C) 2015 Bonitasoft S.A.
 * Bonitasoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * Bonitasoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or Bonitasoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 **/

package com.bonitasoft.connectors.document.templating;

/**
 * @author Baptiste Mesta
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.bpm.document.DocumentNotFoundException;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.bpm.document.impl.DocumentImpl;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.EngineExecutionContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.bonitasoft.engine.api.ProcessAPI;

@RunWith(MockitoJUnitRunner.class)
public class DocumentTemplatingTest {

    private final long processInstanceId = 4861356546l;
    @Mock
    private APIAccessor apiAccessor;
    @Mock
    private EngineExecutionContext engineExecutionContext;
    @Mock
    private ProcessAPI processAPI;
    @Spy
    @InjectMocks
    private DocumentTemplating documentTemplating;

    @Before
    public void before() {
        doReturn(processAPI).when(apiAccessor).getProcessAPI();
        doReturn(processInstanceId).when(engineExecutionContext).getProcessInstanceId();
    }

    @Test
    public void should_execute_return_result_of_convert_method() throws ConnectorException, DocumentNotFoundException {
        //given
        DocumentImpl document = new DocumentImpl();
        document.setContentMimeType("theMimeType");
        document.setFileName("doc.docx");
        document.setContentStorageId("TheStorageID");
        byte[] content = new byte[] { 4, 5, 6 };
        final byte[] contentAfter = { 1, 2, 3 };
        final Map<String, String> replacements = Collections.singletonMap("theKey", "theValue");
        doReturn(contentAfter).when(documentTemplating).applyReplacements(content, replacements);
        doReturn(document).when(processAPI).getLastDocument(processInstanceId, "documentName");
        doReturn(content).when(processAPI).getDocumentContent("TheStorageID");

        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(DocumentTemplating.INPUT_DOCUMENT_INPUT, "documentName");
        parameters.put(DocumentTemplating.INPUT_REPLACEMENTS, replacements);

        documentTemplating.setInputParameters(parameters);

        //when
        final Map<String, Object> execute = documentTemplating.execute();

        //then
        assertThat(execute).containsOnlyKeys(DocumentTemplating.OUTPUT_DOCUMENT);
        assertThat(execute.get(DocumentTemplating.OUTPUT_DOCUMENT)).isEqualToComparingFieldByField(new DocumentValue(contentAfter, "theMimeType", "doc.docx"));
    }

    @Test(expected = ConnectorException.class)
    public void should_execute_throw_exception_when_document_not_found() throws ConnectorException, DocumentNotFoundException {
        //given
        final Map<String, String> replacements = Collections.singletonMap("theKey", "theValue");
        doThrow(new DocumentNotFoundException("")).when(processAPI).getLastDocument(anyLong(), anyString());
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(DocumentTemplating.INPUT_DOCUMENT_INPUT, "documentName");
        parameters.put(DocumentTemplating.INPUT_REPLACEMENTS, replacements);
        documentTemplating.setInputParameters(parameters);

        //when
        documentTemplating.execute();
    }

}
