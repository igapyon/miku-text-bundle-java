package jp.igapyon.mikutextbundle.core;

/**
 * Core facade for miku-text-bundle.
 */
public final class MikuTextBundle {
    public static final String VERSION = "0.8.0";

    private MikuTextBundle() {
    }

    public static String productName() {
        return "miku-text-bundle-java";
    }
}
