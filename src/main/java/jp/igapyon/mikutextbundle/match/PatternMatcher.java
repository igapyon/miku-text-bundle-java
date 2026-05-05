package jp.igapyon.mikutextbundle.match;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jp.igapyon.mikutextbundle.pathutils.PathUtils;

public final class PatternMatcher {
    private PatternMatcher() {
    }

    public static List<String> parseGitignore(String content) {
        List<String> patterns = new ArrayList<String>();
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 0 && !trimmed.startsWith("#") && !trimmed.startsWith("!")) {
                patterns.add(trimmed);
            }
        }
        return patterns;
    }

    public static boolean matchesAnyPattern(String relativePath, List<String> patterns) {
        String normalizedPath = PathUtils.normalizePattern(relativePath);
        for (String pattern : patterns) {
            String normalizedPattern = PathUtils.normalizePattern(pattern);
            if (!normalizedPattern.contains("*")) {
                String directoryPattern = stripTrailingSlash(normalizedPattern);
                if (normalizedPath.equals(normalizedPattern) || normalizedPath.startsWith(directoryPattern + "/")) {
                    return true;
                }
            } else if (globToRegex(normalizedPattern).matcher(normalizedPath).matches()) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesGitignore(String relativePath, List<String> patterns) {
        String normalizedPath = PathUtils.normalizePattern(relativePath);
        for (String pattern : patterns) {
            boolean rootAnchored = pattern.trim().startsWith("/");
            String normalizedPattern = PathUtils.normalizePattern(pattern.replaceFirst("^/", ""));
            boolean directoryOnly = normalizedPattern.endsWith("/");
            String cleanPattern = stripTrailingSlash(normalizedPattern);

            if (cleanPattern.length() == 0) {
                continue;
            }

            if (!cleanPattern.contains("/")) {
                if (matchesBasenamePattern(normalizedPath, cleanPattern, rootAnchored, directoryOnly)) {
                    return true;
                }
            } else if (matchesPathPattern(normalizedPath, cleanPattern, rootAnchored, directoryOnly)) {
                return true;
            }
        }
        return false;
    }

    private static String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static boolean matchesBasenamePattern(String normalizedPath, String cleanPattern, boolean rootAnchored,
            boolean directoryOnly) {
        String[] segments = normalizedPath.split("/");

        if (directoryOnly) {
            if (rootAnchored) {
                return segments.length > 0 && segments[0].equals(cleanPattern);
            }
            for (int i = 0; i < Math.max(0, segments.length - 1); i++) {
                if (segments[i].equals(cleanPattern)) {
                    return true;
                }
            }
            return false;
        }

        if (cleanPattern.contains("*")) {
            Pattern regex = globToRegex(cleanPattern);
            for (String segment : segments) {
                if (regex.matcher(segment).matches()) {
                    return true;
                }
            }
            return false;
        }

        if (rootAnchored) {
            return normalizedPath.equals(cleanPattern);
        }

        for (String segment : segments) {
            if (segment.equals(cleanPattern)) {
                return true;
            }
        }
        return normalizedPath.endsWith("/" + cleanPattern);
    }

    private static boolean matchesPathPattern(String normalizedPath, String cleanPattern, boolean rootAnchored,
            boolean directoryOnly) {
        if (directoryOnly) {
            if (rootAnchored) {
                return normalizedPath.equals(cleanPattern) || normalizedPath.startsWith(cleanPattern + "/");
            }
            return normalizedPath.equals(cleanPattern) || normalizedPath.contains("/" + cleanPattern + "/")
                    || normalizedPath.startsWith(cleanPattern + "/");
        }

        if (cleanPattern.contains("*")) {
            Pattern regex = globToRegex(cleanPattern);
            if (rootAnchored) {
                return regex.matcher(normalizedPath).matches();
            }
            return regex.matcher(normalizedPath).matches() || globToRegex("**/" + cleanPattern).matcher(normalizedPath).matches();
        }

        if (rootAnchored) {
            return normalizedPath.equals(cleanPattern) || normalizedPath.startsWith(cleanPattern + "/");
        }

        return normalizedPath.equals(cleanPattern) || normalizedPath.endsWith("/" + cleanPattern)
                || normalizedPath.startsWith(cleanPattern + "/");
    }

    private static Pattern globToRegex(String pattern) {
        String normalized = PathUtils.normalizePattern(pattern);
        StringBuilder source = new StringBuilder();

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            char next = i + 1 < normalized.length() ? normalized.charAt(i + 1) : 0;
            char afterNext = i + 2 < normalized.length() ? normalized.charAt(i + 2) : 0;

            if (c == '*' && next == '*' && afterNext == '/') {
                source.append("(?:.*/)?");
                i += 2;
                continue;
            }
            if (c == '*' && next == '*') {
                source.append(".*");
                i += 1;
                continue;
            }
            if (c == '*') {
                source.append("[^/]*");
                continue;
            }
            source.append(escapeRegex(c));
        }

        return Pattern.compile("^" + source.toString() + "$");
    }

    private static String escapeRegex(char c) {
        if ("|\\{}()[\\]^$+?.".indexOf(c) >= 0) {
            return "\\" + c;
        }
        return String.valueOf(c);
    }
}
