package jp.igapyon.mikutextbundle.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MikuTextBundleCliIntegrationTest {
    private static final String INDEX_FILE_NAME = "text-bundle-999-index.md";
    private static final String PROMPT_FILE_NAME = "text-bundle-000-prompt.md";
    private static final String FIRST_PART_FILE_NAME = "text-bundle-001.md";

    @TempDir
    Path tempDir;

    @Test
    void generatesMarkdownBundleFilesThroughCli() throws Exception {
        Path output = tempDir.resolve("out");
        write("README.md", "# README\n");
        write("src/main.ts", "const value = 1;\n");

        CliResult result = run("--input", tempDir.toString(), "--output", output.toString(), "--max-chars", "120000");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("completed:"));
        assertTrue(result.out.contains("file(s) skipped"));
        assertTrue(result.out.contains("directories ignored"));
        assertTrue(result.out.contains("file(s) ignored"));
        assertTrue(read(output.resolve(INDEX_FILE_NAME)).contains("src/main.ts"));
        assertTrue(read(output.resolve(FIRST_PART_FILE_NAME)).contains("### src/main.ts"));
        assertTrue(read(output.resolve(PROMPT_FILE_NAME)).contains("text-bundle-response.md"));
    }

    @Test
    void appliesMaxInputFileBytesThroughCli() throws Exception {
        Path output = tempDir.resolve("out");
        write("README.md", "# README\n");
        write("docs/huge.md", repeat("x", 101));

        CliResult result = run("--input", tempDir.toString(), "--output", output.toString(),
                "--max-input-file-bytes", "100");

        String index = read(output.resolve(INDEX_FILE_NAME));
        String part = read(output.resolve(FIRST_PART_FILE_NAME));
        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("completed:"));
        assertTrue(index.contains("`docs/huge.md`"));
        assertTrue(index.contains("100 bytes"));
        assertFalse(part.contains("docs/huge.md"));
    }

    @Test
    void appliesFilenamePrefixThroughCli() throws Exception {
        Path output = tempDir.resolve("out");
        write("README.md", "# README\n");

        CliResult result = run("--input", tempDir.toString(), "--output", output.toString(),
                "--filename-prefix", "sample-repo-text-bundle");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("sample-repo-text-bundle-000-prompt.md"));
        assertTrue(read(output.resolve("sample-repo-text-bundle-000-prompt.md"))
                .contains("sample-repo-text-bundle-999-index.md"));
        assertTrue(read(output.resolve("sample-repo-text-bundle-001.md")).contains("### README.md"));
        assertTrue(read(output.resolve("sample-repo-text-bundle-999-index.md"))
                .contains("sample-repo-text-bundle-001.md"));
    }

    @Test
    void invalidInputDirectoryReturnsUsageError() {
        CliResult result = run("--input", tempDir.resolve("missing").toString(), "--output",
                tempDir.resolve("out").toString());

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Input directory does not exist"));
        assertTrue(result.out.contains("Usage:"));
    }

    @Test
    void unknownOptionReturnsUsageErrorThroughCli() throws Exception {
        write("README.md", "# README\n");

        CliResult result = run("--input", tempDir.toString(), "--output", tempDir.resolve("out").toString(),
                "--unknown");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Unknown argument: --unknown"));
        assertTrue(result.out.contains("Usage:"));
    }

    @Test
    void namedDirectoryOptionsGenerateFilesAndVerboseDiagnostics() throws Exception {
        Path output = tempDir.resolve("named-out");
        write("README.md", "# README\n");
        write("src/main.ts", "const value = 1;\n");

        CliResult result = run("--input", tempDir.toString(), "--output", output.toString(), "--verbose");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("collected=2"));
        assertTrue(result.out.contains("skipped=0"));
        assertTrue(result.out.contains("parts=1"));
        assertTrue(result.out.contains("ignoredDirectories=1"));
        assertTrue(result.out.contains("ignoredByOutputDirectory=0"));
        assertTrue(result.out.contains("generated: " + output.resolve(INDEX_FILE_NAME)));
        assertTrue(result.out.contains("completed: 1 part(s), 2 file(s) collected, 0 file(s) skipped"));
        assertTrue(Files.isRegularFile(output.resolve(INDEX_FILE_NAME)));
        assertTrue(Files.isRegularFile(output.resolve(PROMPT_FILE_NAME)));
        assertTrue(Files.isRegularFile(output.resolve(FIRST_PART_FILE_NAME)));
        assertEquals("", result.err);
    }

    @Test
    void invalidNumericOptionReturnsUsageErrorThroughCli() throws Exception {
        write("README.md", "# README\n");

        CliResult result = run("--input", tempDir.toString(), "--output", tempDir.resolve("out").toString(),
                "--max-chars", "0");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("--max-chars must be a positive integer."));
        assertTrue(result.out.contains("Usage:"));
    }

    @Test
    void missingOptionValueReturnsUsageErrorThroughCli() throws Exception {
        write("README.md", "# README\n");

        CliResult result = run("--input", tempDir.toString(), "--output", tempDir.resolve("out").toString(),
                "--add-exclude-extension");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Please specify a value for --add-exclude-extension."));
        assertTrue(result.out.contains("Usage:"));
    }

    private CliResult run(String... args) {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        int exitCode = MikuTextBundleCli.run(args, new PrintStream(outBytes), new PrintStream(errBytes));
        return new CliResult(exitCode, toString(outBytes), toString(errBytes));
    }

    private void write(String relativePath, String content) throws IOException {
        Path path = tempDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private String toString(ByteArrayOutputStream bytes) {
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private static final class CliResult {
        private final int exitCode;
        private final String out;
        private final String err;

        private CliResult(int exitCode, String out, String err) {
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }
    }
}
