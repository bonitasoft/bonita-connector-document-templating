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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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

import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.ConverterTypeVia;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.TemplateEngineKind;

@RunWith(MockitoJUnitRunner.class)
public class DocumentTemplatingTest {

    private final long processInstanceId = 4861356546L;
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
        final List<List<Object>> replacements = Collections.singletonList(Arrays.asList("theKey", (Object) "theValue"));
        doReturn(contentAfter).when(documentTemplating).applyReplacements(content, replacements, false);
        doReturn(document).when(processAPI).getLastDocument(processInstanceId, "documentName");
        doReturn(content).when(processAPI).getDocumentContent("TheStorageID");

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(DocumentTemplating.INPUT_DOCUMENT_INPUT, "documentName");
        parameters.put(DocumentTemplating.INPUT_REPLACEMENTS, replacements);

        documentTemplating.setInputParameters(parameters);

        //when
        final Map<String, Object> execute = documentTemplating.execute();

        //then
        assertThat(execute).containsOnlyKeys(DocumentTemplating.OUTPUT_DOCUMENT);
        assertThat(execute.get(DocumentTemplating.OUTPUT_DOCUMENT))
                .isEqualToComparingFieldByField(new DocumentValue(contentAfter, "theMimeType", "doc.docx"));
    }

    @Test
    public void should_execute_return_result_of_convert_method_with_outputFileName()
            throws ConnectorException, DocumentNotFoundException {
        //given
        DocumentImpl document = new DocumentImpl();
        document.setContentMimeType("theMimeType");
        document.setFileName("doc.docx");
        document.setContentStorageId("TheStorageID");
        byte[] content = new byte[] { 4, 5, 6 };
        final byte[] contentAfter = { 1, 2, 3 };
        final List<List<Object>> replacements = Collections.singletonList(Arrays.asList("theKey", (Object) "theValue"));
        doReturn(contentAfter).when(documentTemplating).applyReplacements(content, replacements, false);
        doReturn(document).when(processAPI).getLastDocument(processInstanceId, "documentName");
        doReturn(content).when(processAPI).getDocumentContent("TheStorageID");

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(DocumentTemplating.INPUT_DOCUMENT_INPUT, "documentName");
        parameters.put(DocumentTemplating.INPUT_REPLACEMENTS, replacements);
        parameters.put(DocumentTemplating.INPUT_RESULTING_DOC_FILENAME, "outDocument.docx");

        documentTemplating.setInputParameters(parameters);

        //when
        final Map<String, Object> execute = documentTemplating.execute();

        //then
        assertThat(execute).containsOnlyKeys(DocumentTemplating.OUTPUT_DOCUMENT);
        assertThat(execute.get(DocumentTemplating.OUTPUT_DOCUMENT))
                .isEqualToComparingFieldByField(new DocumentValue(contentAfter, "theMimeType", "outDocument.docx"));
    }

    @Test(expected = ConnectorException.class)
    public void should_execute_throw_exception_when_document_not_found()
            throws ConnectorException, DocumentNotFoundException {
        //given
        final Map<String, String> replacements = Collections.singletonMap("theKey", "theValue");
        doThrow(new DocumentNotFoundException("")).when(processAPI).getLastDocument(anyLong(), anyString());
        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(DocumentTemplating.INPUT_DOCUMENT_INPUT, "documentName");
        parameters.put(DocumentTemplating.INPUT_REPLACEMENTS, replacements);
        documentTemplating.setInputParameters(parameters);

        //when
        documentTemplating.execute();
    }

    @Test
    public void process_docx_document()
            throws ConnectorException, DocumentNotFoundException, IOException, XDocReportException {
        //given
        DocumentImpl document = new DocumentImpl();
        document.setContentMimeType("theMimeType");
        document.setFileName("doc.docx");
        document.setContentStorageId("TheStorageID");
        byte[] content = IOUtils.toByteArray(this.getClass().getResourceAsStream("/velocitytest.docx"));

        final List<List<Object>> replacements = new ArrayList<>();

        replacements.add(Arrays.asList("champ", (Object) "FIELD"));
        replacements.add(Arrays.asList("espace", (Object) "SPPPPPAAAAAAACeeee"));
        replacements.add(Arrays.asList("MyField", (Object) "Mon champ :)\n toto"));
        Project project = new Project("The project name");
        replacements.add(Arrays.asList("project", project));
        doReturn(document).when(processAPI).getLastDocument(processInstanceId, "documentName");
        doReturn(content).when(processAPI).getDocumentContent("TheStorageID");

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(DocumentTemplating.INPUT_DOCUMENT_INPUT, "documentName");
        parameters.put(DocumentTemplating.INPUT_REPLACEMENTS, replacements);

        documentTemplating.setInputParameters(parameters);

        //when
        final Map<String, Object> execute = documentTemplating.execute();

        //then
        assertThat(execute).containsOnlyKeys(DocumentTemplating.OUTPUT_DOCUMENT);
        final IXDocReport report = XDocReportRegistry.getRegistry().loadReport(
                new ByteArrayInputStream(((DocumentValue) execute.get(DocumentTemplating.OUTPUT_DOCUMENT)).getContent()),
                TemplateEngineKind.Velocity);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Options options = Options.getTo(ConverterTypeTo.XHTML).via(
                    ConverterTypeVia.XWPF);
            report.convert(report.createContext(), options, out);
            final String actual = new String(out.toByteArray());
            assertThat(actual.contains("mon FIELD avec SPPPPPAAAAAAACeeee")).isTrue();
            assertThat(actual.contains("The project name")).isTrue();
            assertThat(actual.contains("Mon champ :)")).isTrue();
            assertThat(actual.contains("toto")).isTrue();
            assertThat(actual.contains("my task")).isTrue();
            assertThat(actual.contains("[my task, another task, last task]")).isTrue();
            assertThat(actual.contains("[another task, last task, my task]")).isTrue();
        }
    }

    @Test
    public void should_sanitize_input_with_invalid_char_for_docx() throws Exception {
        //given
        DocumentImpl document = new DocumentImpl();
        document.setContentMimeType("theMimeType");
        document.setFileName("template.docx");
        document.setContentStorageId("TheStorageID");
        byte[] content = IOUtils.toByteArray(this.getClass().getResourceAsStream("/template.docx"));

        List<List<Object>> replacements = new ArrayList<>();
        replacements.add(Arrays.asList("field", (Object) "invalidchar")); // There is an invalid char between 'd' and 'c' -> 0x19 invalidchar
        doReturn(document).when(processAPI).getLastDocument(processInstanceId, "documentName");
        doReturn(content).when(processAPI).getDocumentContent("TheStorageID");

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(DocumentTemplating.INPUT_DOCUMENT_INPUT, "documentName");
        parameters.put(DocumentTemplating.INPUT_REPLACEMENTS, replacements);

        documentTemplating.setInputParameters(parameters);

        //when
        Map<String, Object> res = documentTemplating.execute();

        //then
        assertThat(res).containsOnlyKeys(DocumentTemplating.OUTPUT_DOCUMENT);
        IXDocReport report = XDocReportRegistry.getRegistry().loadReport(
                new ByteArrayInputStream(((DocumentValue) res.get(DocumentTemplating.OUTPUT_DOCUMENT)).getContent()),
                TemplateEngineKind.Velocity);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Options options = Options.getTo(ConverterTypeTo.XHTML).via(ConverterTypeVia.XWPF);
            report.convert(report.createContext(), options, out);
            String actual = new String(out.toByteArray());
            assertThat(actual).doesNotContain("invalidchar");// There is an invalid char between 'd' and 'c' -> 0x19 invalidchar
            assertThat(actual).contains("invalidchar");
        }
    }

    @Test
    public void should_sanitize_input_with_invalid_char_for_odt() throws Exception {
        //given
        DocumentImpl document = new DocumentImpl();
        document.setContentMimeType("theMimeType");
        document.setFileName("template.odt");
        document.setContentStorageId("TheStorageID");
        byte[] content = IOUtils.toByteArray(this.getClass().getResourceAsStream("/template.odt"));

        List<List<Object>> replacements = new ArrayList<>();
        replacements.add(Arrays.asList("field", (Object) "invalidchar")); // There is an invalid char between 'd' and 'c' -> 0x19 invalidchar
        doReturn(document).when(processAPI).getLastDocument(processInstanceId, "documentName");
        doReturn(content).when(processAPI).getDocumentContent("TheStorageID");

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(DocumentTemplating.INPUT_DOCUMENT_INPUT, "documentName");
        parameters.put(DocumentTemplating.INPUT_REPLACEMENTS, replacements);

        documentTemplating.setInputParameters(parameters);

        //when
        Map<String, Object> res = documentTemplating.execute();

        //then
        assertThat(res).containsOnlyKeys(DocumentTemplating.OUTPUT_DOCUMENT);
        IXDocReport report = XDocReportRegistry.getRegistry().loadReport(
                new ByteArrayInputStream(((DocumentValue) res.get(DocumentTemplating.OUTPUT_DOCUMENT)).getContent()),
                TemplateEngineKind.Velocity);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Options options = Options.getTo(ConverterTypeTo.XHTML);
            report.convert(report.createContext(), options, out);
            String actual = new String(out.toByteArray());
            assertThat(actual).doesNotContain("invalidchar");// There is an invalid char between 'd' and 'c' -> 0x19 invalidchar
            assertThat(actual).contains("invalidchar");
        }
    }

    public class Project {

        private String name;

        public Project(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<String> getTasks() {
            return Arrays.asList("my task", "another task", "last task");
        }
    }

}
