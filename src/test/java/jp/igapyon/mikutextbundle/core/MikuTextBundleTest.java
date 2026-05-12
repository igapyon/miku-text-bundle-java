package jp.igapyon.mikutextbundle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MikuTextBundleTest {
    @Test
    void exposesProductMetadataForJavaEntrypoints() {
        assertEquals("miku-text-bundle-java", MikuTextBundle.productName());
        assertEquals("0.8.0", MikuTextBundle.VERSION);
    }
}
