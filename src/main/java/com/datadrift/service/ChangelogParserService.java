package com.datadrift.service;

import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.parser.xml.XmlChangelogParser;
import com.datadrift.parser.yaml.YamlChangelogParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * Service for parsing XML and YAML changelog files into ChangeSet objects.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangelogParserService {

    private final XmlChangelogParser xmlParser;
    private final YamlChangelogParser yamlParser;

    /**
     * Parse all changelog files from the db/changelog directory.
     *
     * Should:
     * 1. Read all files from src/main/resources/db/changelog directory
     * 2. Sort files by name (to ensure consistent execution order)
     * 3. For each file, call parseFile() to parse it
     * 4. Combine all changesets from all files into a single list
     * 5. Return the complete list of changesets
     */
    public List<ChangeSet> parseAllChangelogs() {
        // TODO: Implement parsing of all changelog files
        return null;
    }

    /**
     * Parse a single changelog file based on its extension.
     *
     * Should:
     * 1. Check file extension (.xml, .yaml, .yml)
     * 2. Delegate to appropriate parser (xmlParser or yamlParser)
     * 3. Return list of changesets from the file
     * 4. Throw exception if file format is unsupported
     */
    public List<ChangeSet> parseFile(File file) {
        // TODO: Implement file parsing based on extension
        return null;
    }
}
