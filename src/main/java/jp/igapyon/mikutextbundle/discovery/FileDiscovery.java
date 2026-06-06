package jp.igapyon.mikutextbundle.discovery;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jp.igapyon.mikutextbundle.match.PatternMatcher;
import jp.igapyon.mikutextbundle.model.CliOptions;
import jp.igapyon.mikutextbundle.model.IgnoreStats;
import jp.igapyon.mikutextbundle.pathutils.PathUtils;

public final class FileDiscovery {
    public static final List<String> DEFAULT_EXCLUDE_EXTENSIONS = Collections.unmodifiableList(Arrays.asList(
            ".7z", ".aac", ".avi", ".bmp", ".bz2", ".class", ".db", ".dll", ".doc", ".docx", ".dylib",
            ".exe", ".flac", ".gif", ".gz", ".ico", ".jar", ".jpeg", ".jpg", ".m4a", ".mkv", ".mov",
            ".mp3", ".mp4", ".ogg", ".otf", ".parquet", ".pdf", ".png", ".ppt", ".pptx", ".rar", ".so",
            ".sqlite", ".svgz", ".tar", ".tgz", ".tiff", ".ttf", ".war", ".wav", ".webm", ".webp", ".woff",
            ".woff2", ".xls", ".xlsx", ".xz", ".zip"));

    public static final List<String> DEFAULT_EXCLUDE_DIRECTORIES = Collections.unmodifiableList(Arrays.asList(
            ".git", ".codex", ".vscode", ".idea", "node_modules", "dist", "build", "target", "coverage",
            "workplace", "tmp", "temp"));

    private FileDiscovery() {
    }

    public static DiscoveryResult discoverCandidateFiles(Path inputPath, Path outputPath, CliOptions options,
            List<String> gitignorePatterns) throws IOException {
        Set<String> excludeExtensions = getEffectiveExcludeExtensions(options);
        Set<String> excludeDirectories = getEffectiveExcludeDirectories(options);
        DiscoveryResult result = new DiscoveryResult();
        List<Path> candidates = listFilesRecursively(inputPath, inputPath, outputPath, excludeDirectories, result.ignored);

        for (Path filePath : candidates) {
            if (shouldCollectCandidate(inputPath, outputPath, filePath, gitignorePatterns, excludeExtensions,
                    result.ignored)) {
                result.files.add(filePath);
            }
        }

        Collections.sort(result.files, new Comparator<Path>() {
            public int compare(Path left, Path right) {
                return PathUtils.compareUtf16CodeUnits(relativeDiscoveryPath(inputPath, left),
                        relativeDiscoveryPath(inputPath, right));
            }
        });
        return result;
    }

    private static Set<String> getEffectiveExcludeExtensions(CliOptions options) {
        List<String> source = options.excludeExtensions == null
                ? DEFAULT_EXCLUDE_EXTENSIONS
                : options.excludeExtensions;
        Set<String> result = new LinkedHashSet<String>();
        for (String extension : source) {
            result.add(extension.toLowerCase());
        }
        return result;
    }

    private static Set<String> getEffectiveExcludeDirectories(CliOptions options) {
        List<String> source = options.excludeDirectories == null
                ? DEFAULT_EXCLUDE_DIRECTORIES
                : options.excludeDirectories;
        return new LinkedHashSet<String>(source);
    }

    private static boolean isExcludedDirectory(String relativePath, Set<String> excludeDirectories) {
        String normalizedPath = PathUtils.toPosixPath(relativePath).replaceAll("/+$", "");
        String[] segments = normalizedPath.split("/");

        for (String excludedDirectory : excludeDirectories) {
            if (excludedDirectory.indexOf('/') >= 0) {
                if (normalizedPath.equals(excludedDirectory) || normalizedPath.startsWith(excludedDirectory + "/")) {
                    return true;
                }
                continue;
            }

            for (String segment : segments) {
                if (segment.length() > 0 && segment.equals(excludedDirectory)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasExcludedExtension(Path filePath, Set<String> excludeExtensions) {
        String extension = finalExtension(filePath.toString()).toLowerCase();
        return extension.length() > 0 && excludeExtensions.contains(extension);
    }

    private static boolean isOutputPathInsideInput(Path inputPath, Path outputPath) {
        String outputRelativePath = relativeDiscoveryPath(inputPath, outputPath);
        return !outputRelativePath.startsWith("../") && !outputRelativePath.equals("..")
                && outputRelativePath.length() > 0;
    }

    private static boolean isInsideOutputDirectory(Path inputPath, Path filePath, Path outputPath) {
        if (!isOutputPathInsideInput(inputPath, outputPath)) {
            return false;
        }
        String outputRelativePath = relativeDiscoveryPath(inputPath, outputPath);
        String fileRelativePath = relativeDiscoveryPath(inputPath, filePath);
        return fileRelativePath.equals(outputRelativePath) || fileRelativePath.startsWith(outputRelativePath + "/");
    }

    private static Counts countDirectoryTree(Path startPath) throws IOException {
        Counts counts = new Counts();
        counts.directories = 1;
        DirectoryStream<Path> stream = Files.newDirectoryStream(startPath);
        try {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    Counts childCounts = countDirectoryTree(entry);
                    counts.directories += childCounts.directories;
                    counts.files += childCounts.files;
                } else if (Files.isRegularFile(entry)) {
                    counts.files += 1;
                }
            }
        } finally {
            stream.close();
        }
        return counts;
    }

    private static void ignoreDirectory(Path fullPath, IgnoreStats ignored, IgnoreReason reason) throws IOException {
        Counts counts = countDirectoryTree(fullPath);
        ignored.directories += counts.directories;
        addIgnoredFiles(ignored, counts.files, reason);
    }

    private static List<Path> listFilesRecursively(Path rootPath, Path startPath, Path outputPath,
            Set<String> excludeDirectories, IgnoreStats ignored) throws IOException {
        List<Path> files = new ArrayList<Path>();
        List<Path> entries = new ArrayList<Path>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(startPath);
        try {
            for (Path entry : stream) {
                entries.add(entry);
            }
        } finally {
            stream.close();
        }
        Collections.sort(entries, new Comparator<Path>() {
            public int compare(Path left, Path right) {
                return PathUtils.compareUtf16CodeUnits(left.getFileName().toString(), right.getFileName().toString());
            }
        });

        for (Path entry : entries) {
            String relativePath = relativeDiscoveryPath(rootPath, entry);
            if (Files.isDirectory(entry)) {
                if (isInsideOutputDirectory(rootPath, entry, outputPath)) {
                    ignoreDirectory(entry, ignored, IgnoreReason.OUTPUT_DIRECTORY);
                    continue;
                }
                if (isExcludedDirectory(relativePath, excludeDirectories)) {
                    ignoreDirectory(entry, ignored, IgnoreReason.DIRECTORY);
                    continue;
                }
                files.addAll(listFilesRecursively(rootPath, entry, outputPath, excludeDirectories, ignored));
            } else if (Files.isRegularFile(entry)) {
                files.add(entry);
            }
        }
        return files;
    }

    private static boolean shouldCollectCandidate(Path inputPath, Path outputPath, Path filePath,
            List<String> gitignorePatterns, Set<String> excludeExtensions, IgnoreStats ignored) {
        String relativePath = relativeDiscoveryPath(inputPath, filePath);
        if (isInsideOutputDirectory(inputPath, filePath, outputPath)) {
            addIgnoredFiles(ignored, 1, IgnoreReason.OUTPUT_DIRECTORY);
            return false;
        }
        if (hasExcludedExtension(filePath, excludeExtensions)) {
            addIgnoredFiles(ignored, 1, IgnoreReason.EXTENSION);
            return false;
        }
        if (PatternMatcher.matchesGitignore(relativePath, gitignorePatterns)) {
            addIgnoredFiles(ignored, 1, IgnoreReason.GITIGNORE);
            return false;
        }
        return true;
    }

    private static void addIgnoredFiles(IgnoreStats ignored, int count, IgnoreReason reason) {
        ignored.files += count;
        if (reason == IgnoreReason.DIRECTORY) {
            ignored.byDirectory += count;
        } else if (reason == IgnoreReason.EXTENSION) {
            ignored.byExtension += count;
        } else if (reason == IgnoreReason.GITIGNORE) {
            ignored.byGitignore += count;
        } else if (reason == IgnoreReason.OUTPUT_DIRECTORY) {
            ignored.byOutputDirectory += count;
        }
    }

    private static String relativeDiscoveryPath(Path inputPath, Path filePath) {
        return PathUtils.toPosixPath(inputPath.relativize(filePath).toString());
    }

    private static String finalExtension(String pathValue) {
        String normalized = PathUtils.toPosixPath(pathValue);
        int slashIndex = normalized.lastIndexOf('/');
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex <= slashIndex || dotIndex == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(dotIndex);
    }

    private enum IgnoreReason {
        DIRECTORY,
        EXTENSION,
        GITIGNORE,
        OUTPUT_DIRECTORY
    }

    private static final class Counts {
        private int directories;
        private int files;
    }
}
