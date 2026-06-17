package jp.igapyon.mikutextbundle.core;

/**
 * Core facade for miku-text-bundle.
 */
public final class MikuTextBundle {
    public static final String VERSION = "1.1.1";

    private MikuTextBundle() {
    }

    public static String productName() {
        return "miku-text-bundle-java";
    }
}
