package jp.igapyon.mikutextbundle.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikutextbundle.model.CliOptions;

class MikuTextBundleCliTest {
    @Test
    void helpReturnsUsage() {
        CliResult result = run("--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("miku-text-bundle <inputDir>"));
        assertTrue(result.out.contains("--max-chars"));
        assertEquals("", result.err);
    }

    @Test
    void versionReturnsProductVersion() {
        CliResult result = run("--version");

        assertEquals(0, result.exitCode);
        assertEquals("miku-text-bundle-java 0.5.0\n", result.out);
        assertEquals("", result.err);
    }

    @Test
    void parseArgsParsesPositionalArgumentsAndOptions() throws Exception {
        CliOptions options = MikuTextBundleCli.parseArgs(new String[] { ".", "out", "--max-chars", "1000",
                "--max-input-file-bytes", "2000", "--include", "docs/**/*.md,package.json", "--exclude", "test/**",
                "--verbose" });

        assertEquals(".", options.inputDirectory);
        assertEquals("out", options.outputDirectory);
        assertEquals(1000, options.maxChars);
        assertEquals(2000, options.maxInputFileBytes);
        assertEquals(Arrays.asList("docs/**/*.md", "package.json"), options.includePatterns);
        assertEquals(Arrays.asList("test/**"), options.excludePatterns);
        assertTrue(options.verbose);
    }

    @Test
    void parseArgsParsesNamedDirectoryOptions() throws Exception {
        CliOptions options = MikuTextBundleCli.parseArgs(new String[] { "--input-directory", ".", "--output-directory", "out" });

        assertEquals(".", options.inputDirectory);
        assertEquals("out", options.outputDirectory);
        assertEquals(120000, options.maxChars);
        assertEquals(1000000, options.maxInputFileBytes);
    }

    @Test
    void missingInputDirectoryReturnsUsageError() {
        CliResult result = run();

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Please specify an input directory."));
        assertTrue(result.out.contains("miku-text-bundle <inputDir>"));
    }

    @Test
    void unknownOptionReturnsUsageError() {
        CliResult result = run(".", "--unknown");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Unknown argument: --unknown"));
        assertTrue(result.out.contains("miku-text-bundle <inputDir>"));
    }

    private CliResult run(String... args) {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes);
        PrintStream err = new PrintStream(errBytes);

        int exitCode = MikuTextBundleCli.run(args, out, err);

        out.flush();
        err.flush();
        return new CliResult(exitCode, toString(outBytes), toString(errBytes));
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
