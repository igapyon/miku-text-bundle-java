package jp.igapyon.mikutextbundle.pathutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PathUtilsTest {
    @Test
    void extractsLowercaseExtensionWithoutDot() {
        assertEquals("ts", PathUtils.getExtension("src/main.TS"));
        assertEquals("", PathUtils.getExtension("README"));
    }

    @Test
    void normalizesPatterns() {
        assertEquals("docs/**/*.md", PathUtils.normalizePattern("./docs/**/*.md"));
    }
}
