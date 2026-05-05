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
    private static final String INDEX_FILE_NAME = "text-bundle-000-index.md";
    private static final String PROMPT_FILE_NAME = "text-bundle-000-prompt.md";
    private static final String FIRST_PART_FILE_NAME = "text-bundle-001.md";

    @TempDir
    Path tempDir;

    @Test
    void generatesMarkdownBundleFilesThroughCli() throws Exception {
        Path output = tempDir.resolve("out");
        write("README.md", "# README\n");
        write("src/main.ts", "const value = 1;\n");

        CliResult result = run(tempDir.toString(), output.toString(), "--max-chars", "120000");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("completed:"));
        assertTrue(read(output.resolve(INDEX_FILE_NAME)).contains("src/main.ts"));
        assertTrue(read(output.resolve(FIRST_PART_FILE_NAME)).contains("### src/main.ts"));
        assertTrue(read(output.resolve(PROMPT_FILE_NAME)).contains("text-bundle-response.md"));
    }

    @Test
    void appliesMaxInputFileBytesThroughCli() throws Exception {
        Path output = tempDir.resolve("out");
        write("README.md", "# README\n");
        write("docs/huge.md", repeat("x", 101));

        CliResult result = run(tempDir.toString(), output.toString(), "--include", "docs/**/*.md",
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
    void invalidInputDirectoryReturnsUsageError() {
        CliResult result = run(tempDir.resolve("missing").toString());

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Input directory does not exist"));
        assertTrue(result.out.contains("Usage:"));
    }

    @Test
    void unknownOptionReturnsUsageErrorThroughCli() throws Exception {
        write("README.md", "# README\n");

        CliResult result = run(tempDir.toString(), "--unknown");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Unknown argument: --unknown"));
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
