package com.datadrift.service;

import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.parser.ChangelogParser;
import com.datadrift.parser.xml.XmlChangelogParser;
import com.datadrift.parser.yaml.YamlChangelogParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
*   Parsing Flow Summary (Top to Bottom)

  ChangelogParserService.parseAllChangelogs()
    ↓
    Scans classpath:db/changelog/, sorts by filename
    ↓
    For each .xml or .yaml file:
      ↓
      ChangelogParser.parse(file)  // XmlChangelogParser or YamlChangelogParser
        ↓
        Format-specific parsing → ParsedNode tree
        ↓
        For each <changeSet>:
          ↓
          ChangeSetLoader.load(parsedNode)
            ↓
            Extracts changeSet-level attributes
            ↓
            For each child (dropTable, createTable, sql, etc.):
              ↓
              resolveChangeClass(tagName) → DropTableChange.class
              ↓
              PropertyMapper.populate(childNode, DropTableChange.class)
                ↓
                new DropTableChange()
                ↓
                setAttributes() → setTableName(), setSchemaName(), etc.
                ↓
                setTextContent() → e.g., setSql() if <sql>
                ↓
                processChildren() → e.g., populateColumns() if <column> children
                ↓
                Returns populated DropTableChange
              ↓
              Adds to changes list
            ↓
            Returns ChangeSet (with all changes)
          ↓
          Sets filename on ChangeSet
        ↓
        Returns List<ChangeSet>
    ↓
    Combines all ChangeSets from all files
    ↓
    Returns complete List<ChangeSet>

  ---
  - ChangelogParserService → understands files and directories
  - ChangelogParser (interface) → contract for format-specific parsers
  - XmlChangelogParser → understands DOM and XML
  - YamlChangelogParser → understands YAML maps and lists
  - ChangeSetLoader → understands ParsedNode and ChangeSet
  - PropertyMapper → understands ParsedNode and Change subclasses
  */
@Slf4j
@Service
public class ChangelogParserService {

    private final ChangelogParser xmlParser;
    private final ChangelogParser yamlParser;
    private final ResourceLoader resourceLoader;
    private final String changelogDirectory;

    public ChangelogParserService(
            XmlChangelogParser xmlParser,
            YamlChangelogParser yamlParser,
            ResourceLoader resourceLoader,
            @Value("${datadrift.changelog.directory:classpath:db/changelog/}") String changelogDirectory) {
        this.xmlParser = xmlParser;
        this.yamlParser = yamlParser;
        this.resourceLoader = resourceLoader;
        this.changelogDirectory = changelogDirectory;
    }

    public List<ChangeSet> parseAllChangelogs() {
        File directory;
        try {
            Resource dir = resourceLoader.getResource(changelogDirectory);
            directory = dir.getFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to locate changelog directory: " + changelogDirectory, e);
        }

        if (!directory.isDirectory()) {
            throw new RuntimeException(changelogDirectory + " is not a directory");
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            log.warn("No changelog files found in {}", changelogDirectory);
            return List.of();
        }

        List<File> sorted = List.of(files).stream()
                .sorted(Comparator.comparing(File::getName))
                .toList();

        List<ChangeSet> allChangeSets = new ArrayList<>();
        for (File file : sorted) {
            if (isSupportedFile(file)) {
                allChangeSets.addAll(parseFile(file));
            } else if (file.isFile()) {
                log.debug("Skipping unsupported file: {}", file.getName());
            }
        }

        return allChangeSets;
    }

    public List<ChangeSet> parseFile(File file) {
        String name = file.getName().toLowerCase();

        if (name.endsWith(".xml")) {
            return xmlParser.parse(file);
        } else if (name.endsWith(".yaml") || name.endsWith(".yml")) {
            return yamlParser.parse(file);
        } else {
            throw new IllegalArgumentException("Unsupported changelog file format: " + file.getName());
        }
    }

    private boolean isSupportedFile(File file) {
        String name = file.getName().toLowerCase();
        return file.isFile() && (name.endsWith(".xml") || name.endsWith(".yaml") || name.endsWith(".yml"));
    }
}
