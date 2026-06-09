package jp.igapyon.mikutextbundle.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class PomMetadataTest {
    @Test
    void declaresMavenPackageShape() throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("pom.xml"));

        assertEquals("jp.igapyon", text(document, "groupId"));
        assertEquals("miku-text-bundle-java", text(document, "artifactId"));
        assertEquals("1.0.0", text(document, "version"));
        assertEquals("jar", text(document, "packaging"));
        assertEquals("Apache License, Version 2.0", text(document, "name", 1));
        assertEquals("1.8", text(document, "maven.compiler.source"));
        assertEquals("1.8", text(document, "maven.compiler.target"));
    }

    @Test
    void keepsExecutableJarPackagingConfigured() throws Exception {
        String pom = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("pom.xml")),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(pom.contains("maven-shade-plugin"));
        assertTrue(pom.contains("jp.igapyon.mikutextbundle.cli.MikuTextBundleCli"));
        assertTrue(pom.contains("maven-source-plugin"));
        assertTrue(pom.contains("maven-failsafe-plugin"));
        assertTrue(pom.contains("project.build.finalName"));
    }

    private String text(Document document, String tagName) {
        return text(document, tagName, 0);
    }

    private String text(Document document, String tagName, int index) {
        return document.getElementsByTagName(tagName).item(index).getTextContent().trim();
    }
}
