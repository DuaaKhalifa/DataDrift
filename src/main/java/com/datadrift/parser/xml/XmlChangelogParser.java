package com.datadrift.parser.xml;

import com.datadrift.model.changelog.ChangeSet;
import com.datadrift.parser.ChangeSetLoader;
import com.datadrift.parser.ParsedNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class XmlChangelogParser {

    private final ChangeSetLoader changeSetLoader;

    public List<ChangeSet> parse(File xmlFile) {
        log.info("Parsing XML changelog: {}", xmlFile.getName());

        Document doc = parseXml(xmlFile);

        List<ChangeSet> changeSets = new ArrayList<>();
        NodeList changeSetNodes = doc.getElementsByTagName("changeSet");

        for (int i = 0; i < changeSetNodes.getLength(); i++) {
            ParsedNode parsedNode = convertElement((Element) changeSetNodes.item(i));
            ChangeSet changeSet = changeSetLoader.load(parsedNode);
            changeSet.setFilename(xmlFile.getName());
            changeSets.add(changeSet);
        }

        log.info("Parsed {} changeset(s) from {}", changeSets.size(), xmlFile.getName());
        return changeSets;
    }

    private Document parseXml(File xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(xmlFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML file: " + xmlFile.getName(), e);
        }
    }

    private ParsedNode convertElement(Element element) {
        ParsedNode node = new ParsedNode();
        node.setName(element.getTagName());

        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            node.getAttributes().put(attr.getNodeName(), attr.getNodeValue());
        }

        NodeList children = element.getChildNodes();
        StringBuilder textContent = new StringBuilder();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            switch (child.getNodeType()) {
                case Node.ELEMENT_NODE -> node.getChildren().add(convertElement((Element) child));
                case Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> textContent.append(child.getNodeValue());
            }
        }

        String text = textContent.toString().trim();
        if (!text.isEmpty()) {
            node.setValue(text);
        }

        return node;
    }
}
