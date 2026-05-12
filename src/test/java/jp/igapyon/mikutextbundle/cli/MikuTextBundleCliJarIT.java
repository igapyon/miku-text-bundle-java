package jp.igapyon.mikutextbundle.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MikuTextBundleCliJarIT {
    private static final String INDEX_FILE_NAME = "text-bundle-000-index.md";
    private static final String FIRST_PART_FILE_NAME = "text-bundle-001.md";

    @TempDir
    Path tempDir;

    @Test
    void packagedJarGeneratesBundleFiles() throws Exception {
        Path input = tempDir.resolve("input");
        Path output = tempDir.resolve("output");
        write(input.resolve("README.md"), "# README\n");
        write(input.resolve("src/main.ts"), "const value = 1;\n");

        ProcessResult result = runJar("--input", input.toString(), "--output", output.toString(), "--max-chars", "120000");

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("completed: 1 part(s), 2 file(s) collected, 0 file(s) skipped"));
        assertTrue(read(output.resolve(INDEX_FILE_NAME)).contains("`src/main.ts`"));
        assertTrue(read(output.resolve(FIRST_PART_FILE_NAME)).contains("### src/main.ts"));
        assertEquals("", result.stderr);
    }

    @Test
    void packagedJarReturnsNonZeroForInvalidInputDirectory() throws Exception {
        ProcessResult result = runJar("--input", tempDir.resolve("missing").toString(), "--output",
                tempDir.resolve("out").toString());

        assertEquals(1, result.exitCode);
        assertTrue(result.stderr.contains("Input directory does not exist"));
        assertTrue(result.stdout.contains("Usage:"));
    }

    private ProcessResult runJar(String... args) throws Exception {
        Path jarPath = jarPath();
        java.util.List<String> command = new java.util.ArrayList<String>();
        command.add(javaExecutable());
        command.add("-jar");
        command.add(jarPath.toString());
        for (String arg : args) {
            command.add(arg);
        }

        Process process = new ProcessBuilder(command).start();
        byte[] stdout = readAll(process.getInputStream());
        byte[] stderr = readAll(process.getErrorStream());
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode, new String(stdout, StandardCharsets.UTF_8),
                new String(stderr, StandardCharsets.UTF_8));
    }

    private Path jarPath() {
        String buildDirectory = System.getProperty("project.build.directory", "target");
        String finalName = System.getProperty("project.build.finalName", "miku-text-bundle-java-0.8.0");
        return new File(buildDirectory, finalName + ".jar").toPath();
    }

    private String javaExecutable() {
        return new File(System.getProperty("java.home"), "bin/java").toString();
    }

    private void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private byte[] readAll(java.io.InputStream stream) throws Exception {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static final class ProcessResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private ProcessResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
