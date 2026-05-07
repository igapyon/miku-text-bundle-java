package jp.igapyon.mikutextbundle.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikutextbundle.model.CliOptions;
import jp.igapyon.mikutextbundle.model.SupportedEncoding;

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
        assertEquals("miku-text-bundle-java 0.5.3\n", result.out);
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
        assertEquals(SupportedEncoding.UTF_8, options.encoding.defaultEncoding);
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
        assertEquals(SupportedEncoding.UTF_8, options.encoding.defaultEncoding);
    }

    @Test
    void parseArgsParsesDefaultAndExtensionEncodingOptions() throws Exception {
        CliOptions options = MikuTextBundleCli.parseArgs(new String[] { ".", "--encoding", "shift_jis",
                "--encoding-extension", ".ts=utf-8,.java=shift_jis" });

        assertEquals(SupportedEncoding.SHIFT_JIS, options.encoding.defaultEncoding);
        assertEquals(SupportedEncoding.UTF_8, options.encoding.extensions.get(".ts"));
        assertEquals(SupportedEncoding.SHIFT_JIS, options.encoding.extensions.get(".java"));
    }

    @Test
    void invalidEncodingOptionsReturnUsageError() {
        CliResult unsupported = run(".", "--encoding", "latin1");
        CliResult missingDot = run(".", "--encoding-extension", "java=shift_jis");
        CliResult unsupportedExtension = run(".", "--encoding-extension", ".java=latin1");

        assertEquals(1, unsupported.exitCode);
        assertTrue(unsupported.err.contains("--encoding must be one of"));
        assertEquals(1, missingDot.exitCode);
        assertTrue(missingDot.err.contains("leading dot"));
        assertEquals(1, unsupportedExtension.exitCode);
        assertTrue(unsupportedExtension.err.contains("--encoding-extension must be one of"));
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

    @Test
    void missingOptionValueReturnsUsageError() {
        CliResult result = run("--input-directory");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Please specify a value for --input-directory."));
        assertTrue(result.out.contains("miku-text-bundle <inputDir>"));
    }

    @Test
    void invalidPositiveIntegerReturnsUsageError() {
        CliResult result = run(".", "--max-input-file-bytes", "not-a-number");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("--max-input-file-bytes must be a positive integer."));
        assertTrue(result.out.contains("miku-text-bundle <inputDir>"));
    }

    @Test
    void unexpectedPositionalArgumentReturnsUsageError() {
        CliResult result = run(".", "out", "extra");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Unexpected positional argument: extra"));
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
