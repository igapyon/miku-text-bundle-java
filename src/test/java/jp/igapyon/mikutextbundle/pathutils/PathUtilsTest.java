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

    @Test
    void comparesUtf16CodeUnitsWithoutLocaleOrNumericCollation() {
        java.util.List<String> values = new java.util.ArrayList<String>();
        values.add("あ.txt");
        values.add("file-2.txt");
        values.add("b.txt");
        values.add("file-10.txt");
        values.add("A.txt");

        java.util.Collections.sort(values, new java.util.Comparator<String>() {
            public int compare(String left, String right) {
                return PathUtils.compareUtf16CodeUnits(left, right);
            }
        });

        assertEquals(java.util.Arrays.asList("A.txt", "b.txt", "file-10.txt", "file-2.txt", "あ.txt"), values);
    }
}
