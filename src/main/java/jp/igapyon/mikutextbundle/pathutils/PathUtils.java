package jp.igapyon.mikutextbundle.pathutils;

import java.io.File;

public final class PathUtils {
    private PathUtils() {
    }

    public static String toPosixPath(String pathValue) {
        return pathValue.replace(File.separatorChar, '/');
    }

    public static String getExtension(String pathValue) {
        String name = new File(pathValue).getName();
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index + 1).toLowerCase();
    }

    public static String normalizePattern(String pattern) {
        String normalized = toPosixPath(pattern.trim());
        if (normalized.startsWith("./")) {
            return normalized.substring(2);
        }
        return normalized;
    }

    public static int compareUtf16CodeUnits(String left, String right) {
        return left.compareTo(right);
    }
}
