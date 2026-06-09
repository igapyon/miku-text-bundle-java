package jp.igapyon.mikutextbundle.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jp.igapyon.mikutextbundle.model.CliOptions;

class UpstreamParityTest {
    private static final String INDEX_FILE_NAME = "text-bundle-999-index.md";
    private static final String PROMPT_FILE_NAME = "text-bundle-000-prompt.md";
    private static final String FIRST_PART_FILE_NAME = "text-bundle-001.md";
    private static final long FIXED_NOW_MILLIS = 1777914060000L;

    @TempDir
    Path tempDir;

    @Test
    void productFixtureMatchesUpstreamMarkdownOutputs() throws Exception {
        Path upstreamMain = findUpstreamMain();
        assumeTrue(upstreamMain != null, "local upstream 1.0.0 dist/main.js is unavailable");
        assumeTrue(isNodeAvailable(), "node executable is unavailable");

        Path javaInput = tempDir.resolve("java-input");
        Path upstreamInput = tempDir.resolve("upstream-input");
        Path javaOutput = tempDir.resolve("java-output");
        Path upstreamOutput = tempDir.resolve("upstream-output");
        copyResourceDirectory("fixtures/product-repo", javaInput);
        copyResourceDirectory("fixtures/product-repo", upstreamInput);

        CliOptions options = new CliOptions();
        options.inputDirectory = javaInput.toString();
        options.outputDirectory = javaOutput.toString();
        options.maxChars = 120000;
        options.maxInputFileBytes = 1000000;

        new TextBundler().createTextBundle(options, new Date(FIXED_NOW_MILLIS),
                new PrintStream(new ByteArrayOutputStream()));
        runUpstream(upstreamMain, upstreamInput, upstreamOutput);

        assertEquals(normalizeIndex(read(upstreamOutput.resolve(INDEX_FILE_NAME)), upstreamInput, upstreamOutput),
                normalizeIndex(read(javaOutput.resolve(INDEX_FILE_NAME)), javaInput, javaOutput));
        assertEquals(read(upstreamOutput.resolve(FIRST_PART_FILE_NAME)), read(javaOutput.resolve(FIRST_PART_FILE_NAME)));
        assertEquals(read(upstreamOutput.resolve(PROMPT_FILE_NAME)), read(javaOutput.resolve(PROMPT_FILE_NAME)));
    }

    private Path findUpstreamMain() throws IOException {
        java.util.List<Path> candidates = new java.util.ArrayList<Path>();
        String configuredRoot = System.getProperty("mikuTextBundle.upstreamRoot");
        if (configuredRoot != null && configuredRoot.length() > 0) {
            candidates.add(java.nio.file.Paths.get(configuredRoot));
        }
        candidates.add(java.nio.file.Paths.get("workplace/miku-text-bundle-upstream"));
        candidates.add(java.nio.file.Paths.get("workplace/miku-text-bundle-devel"));
        for (Path candidate : candidates) {
            Path main = candidate.resolve("dist/main.js").toAbsolutePath().normalize();
            if (Files.isRegularFile(main) && isUpstreamVersion(candidate, "1.0.0")) {
                return main;
            }
        }
        return null;
    }

    private boolean isUpstreamVersion(Path upstreamRoot, String expectedVersion) throws IOException {
        Path packageJson = upstreamRoot.resolve("package.json");
        if (!Files.isRegularFile(packageJson)) {
            return false;
        }
        return read(packageJson).contains("\"version\": \"" + expectedVersion + "\"");
    }

    private boolean isNodeAvailable() throws InterruptedException {
        try {
            Process process = new ProcessBuilder("node", "--version").start();
            readAll(process.getInputStream());
            readAll(process.getErrorStream());
            return process.waitFor() == 0;
        } catch (IOException ex) {
            return false;
        }
    }

    private void runUpstream(Path upstreamMain, Path inputDirectory, Path outputDirectory)
            throws IOException, InterruptedException {
        String script = "import { createTextBundle } from " + quote(upstreamMain.toUri().toString()) + ";"
                + "createTextBundle({"
                + "inputDirectory:" + quote(inputDirectory.toString()) + ","
                + "outputDirectory:" + quote(outputDirectory.toString()) + ","
                + "maxChars:120000,"
                + "maxInputFileBytes:1000000,"
                + "verbose:false"
                + "}, new Date(" + FIXED_NOW_MILLIS + "));";

        Process process = new ProcessBuilder("node", "--input-type=module", "-e", script).start();
        String stdout = new String(readAll(process.getInputStream()), StandardCharsets.UTF_8);
        String stderr = new String(readAll(process.getErrorStream()), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        assertEquals(0, exitCode, "upstream parity command failed\nstdout:\n" + stdout + "\nstderr:\n" + stderr);
    }

    private String normalizeIndex(String value, Path inputDirectory, Path outputDirectory) {
        return value.replace(path(inputDirectory), "<INPUT>").replace(path(outputDirectory), "<OUTPUT>");
    }

    private String path(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private void copyResourceDirectory(String resourceName, Path destination) throws IOException, URISyntaxException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new IllegalArgumentException("Test resource not found: " + resourceName);
        }
        Path sourceRoot = java.nio.file.Paths.get(resource.toURI());
        java.util.List<Path> paths = new java.util.ArrayList<Path>();
        try (java.util.stream.Stream<Path> stream = Files.walk(sourceRoot)) {
            java.util.Iterator<Path> iterator = stream.iterator();
            while (iterator.hasNext()) {
                paths.add(iterator.next());
            }
        }
        for (Path sourcePath : paths) {
            Path relativePath = sourceRoot.relativize(sourcePath);
            Path targetPath = destination.resolve(relativePath.toString());
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(targetPath);
            } else {
                Files.createDirectories(targetPath.getParent());
                Files.copy(sourcePath, targetPath);
            }
        }
    }

    private String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private byte[] readAll(java.io.InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
