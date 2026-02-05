package com.datadrift.service;

import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.parser.xml.XmlChangelogParser;
import com.datadrift.parser.yaml.YamlChangelogParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangelogParserServiceTest {

    @Mock
    private XmlChangelogParser xmlParser;

    @Mock
    private YamlChangelogParser yamlParser;

    @Mock
    private ResourceLoader resourceLoader;

    @TempDir
    Path tempDir;

    private ChangelogParserService service;

    @BeforeEach
    void setUp() {
        service = new ChangelogParserService(xmlParser, yamlParser, resourceLoader, "classpath:db/changelog/");
    }

    @Test
    void testParseFile_XmlExtension_DelegatesToXmlParser() {
        File file = new File("changelog.xml");
        List<ChangeSet> expected = List.of(new ChangeSet());
        when(xmlParser.parse(file)).thenReturn(expected);

        List<ChangeSet> result = service.parseFile(file);

        assertEquals(expected, result);
        verify(xmlParser).parse(file);
        verifyNoInteractions(yamlParser);
    }

    @Test
    void testParseFile_YamlExtension_DelegatesToYamlParser() {
        File file = new File("changelog.yaml");
        List<ChangeSet> expected = List.of(new ChangeSet());
        when(yamlParser.parse(file)).thenReturn(expected);

        List<ChangeSet> result = service.parseFile(file);

        assertEquals(expected, result);
        verify(yamlParser).parse(file);
        verifyNoInteractions(xmlParser);
    }

    @Test
    void testParseFile_YmlExtension_DelegatesToYamlParser() {
        File file = new File("changelog.yml");
        List<ChangeSet> expected = List.of(new ChangeSet());
        when(yamlParser.parse(file)).thenReturn(expected);

        List<ChangeSet> result = service.parseFile(file);

        assertEquals(expected, result);
        verify(yamlParser).parse(file);
    }

    @Test
    void testParseFile_UppercaseExtension_StillRoutes() {
        File file = new File("CHANGELOG.XML");
        List<ChangeSet> expected = List.of(new ChangeSet());
        when(xmlParser.parse(file)).thenReturn(expected);

        List<ChangeSet> result = service.parseFile(file);

        assertEquals(expected, result);
        verify(xmlParser).parse(file);
    }

    @Test
    void testParseFile_UnsupportedExtension_ThrowsException() {
        File file = new File("readme.txt");

        assertThrows(IllegalArgumentException.class, () -> service.parseFile(file));
    }

    // --- parseAllChangelogs ---

    private Resource mockDirectoryResource() throws IOException {
        Resource mockResource = mock(Resource.class);
        when(mockResource.getFile()).thenReturn(tempDir.toFile());
        when(resourceLoader.getResource("classpath:db/changelog/")).thenReturn(mockResource);
        return mockResource;
    }

    private ChangelogParserService createServiceWithCustomDirectory(String directory) {
        return new ChangelogParserService(xmlParser, yamlParser, resourceLoader, directory);
    }

    @Test
    void testParseAllChangelogs_SortsFilesByName() throws IOException {
        Files.createFile(tempDir.resolve("002-second.xml"));
        Files.createFile(tempDir.resolve("001-first.xml"));
        mockDirectoryResource();

        ChangeSet cs1 = new ChangeSet();
        cs1.setId("001");
        ChangeSet cs2 = new ChangeSet();
        cs2.setId("002");

        when(xmlParser.parse(any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            return file.getName().startsWith("001") ? List.of(cs1) : List.of(cs2);
        });

        List<ChangeSet> result = service.parseAllChangelogs();

        assertEquals(2, result.size());
        assertEquals("001", result.get(0).getId());
        assertEquals("002", result.get(1).getId());
    }

    @Test
    void testParseAllChangelogs_MixedExtensions_RoutesCorrectly() throws IOException {
        Files.createFile(tempDir.resolve("001-first.xml"));
        Files.createFile(tempDir.resolve("002-second.yaml"));
        mockDirectoryResource();

        ChangeSet csXml = new ChangeSet();
        csXml.setId("xml");
        ChangeSet csYaml = new ChangeSet();
        csYaml.setId("yaml");

        when(xmlParser.parse(any(File.class))).thenReturn(List.of(csXml));
        when(yamlParser.parse(any(File.class))).thenReturn(List.of(csYaml));

        List<ChangeSet> result = service.parseAllChangelogs();

        assertEquals(2, result.size());
        verify(xmlParser, times(1)).parse(any());
        verify(yamlParser, times(1)).parse(any());
    }

    @Test
    void testParseAllChangelogs_SkipsUnsupportedFiles() throws IOException {
        Files.createFile(tempDir.resolve("001-first.xml"));
        Files.createFile(tempDir.resolve(".DS_Store"));
        Files.createFile(tempDir.resolve("README.md"));
        mockDirectoryResource();

        when(xmlParser.parse(any(File.class))).thenReturn(List.of(new ChangeSet()));

        List<ChangeSet> result = service.parseAllChangelogs();

        assertEquals(1, result.size());
        verify(xmlParser, times(1)).parse(any());
        verifyNoInteractions(yamlParser);
    }

    @Test
    void testParseAllChangelogs_EmptyDirectory_ReturnsEmptyList() throws IOException {
        mockDirectoryResource();

        List<ChangeSet> result = service.parseAllChangelogs();

        assertTrue(result.isEmpty());
        verifyNoInteractions(xmlParser);
        verifyNoInteractions(yamlParser);
    }

    @Test
    void testParseAllChangelogs_SingleFileMultipleChangesets_CombinesAll() throws IOException {
        Files.createFile(tempDir.resolve("001.xml"));
        mockDirectoryResource();

        ChangeSet cs1 = new ChangeSet();
        cs1.setId("001");
        ChangeSet cs2 = new ChangeSet();
        cs2.setId("002");
        when(xmlParser.parse(any(File.class))).thenReturn(List.of(cs1, cs2));

        List<ChangeSet> result = service.parseAllChangelogs();

        assertEquals(2, result.size());
    }

    @Test
    void testParseAllChangelogs_DirectoryIoError_ThrowsException() throws IOException {
        Resource mockResource = mock(Resource.class);
        when(mockResource.getFile()).thenThrow(new IOException("not found"));
        when(resourceLoader.getResource("classpath:db/changelog/")).thenReturn(mockResource);

        assertThrows(RuntimeException.class, () -> service.parseAllChangelogs());
    }

    @Test
    void testParseAllChangelogs_CustomDirectory_UsesConfiguredPath() throws IOException {
        Files.createFile(tempDir.resolve("001.xml"));

        Resource mockResource = mock(Resource.class);
        when(mockResource.getFile()).thenReturn(tempDir.toFile());
        when(resourceLoader.getResource("classpath:custom/migrations/")).thenReturn(mockResource);
        when(xmlParser.parse(any(File.class))).thenReturn(List.of(new ChangeSet()));

        ChangelogParserService customService = createServiceWithCustomDirectory("classpath:custom/migrations/");
        List<ChangeSet> result = customService.parseAllChangelogs();

        assertEquals(1, result.size());
        verify(resourceLoader).getResource("classpath:custom/migrations/");
    }
}
