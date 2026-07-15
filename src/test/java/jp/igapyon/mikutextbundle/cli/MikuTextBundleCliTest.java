package jp.igapyon.mikutextbundle.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(result.out.contains("miku-text-bundle --input <dir> --output <dir>"));
        assertTrue(result.out.contains("Default behavior:"));
        assertTrue(result.out.contains("--filename-prefix text-bundle"));
        assertTrue(result.out.contains("Generated artifacts:"));
        assertTrue(result.out.contains("<prefix>-001.md ... <prefix>-999.md"));
        assertTrue(result.out.contains("The first part includes the prompt instructions."));
        assertTrue(result.out.contains("The final part includes the terminal index."));
        assertFalse(result.out.contains("For Web UI"));
        assertFalse(result.out.contains("recommended, not required"));
        assertTrue(result.out.contains("Output and overwrite behavior:"));
        assertTrue(result.out.contains("stdout is progress/completion text"));
        assertTrue(result.out.contains("the final part index. Invalid usage"));
        assertTrue(result.out.contains("Exit code 0 means success/help/version"));
        assertTrue(result.out.contains("source-content chars per part"));
        assertTrue(result.out.contains("nested .gitignore files and negation patterns"));
        assertTrue(result.out.contains("--filename-prefix"));
        assertTrue(result.out.contains("--add-exclude-extension"));
        assertTrue(result.out.contains("--dry-run"));
        assertEquals("", result.err);
    }

    @Test
    void versionReturnsPackageVersionOnly() {
        CliResult result = run("--version");

        assertEquals(0, result.exitCode);
        assertEquals("1.4.0\n", result.out);
        assertEquals("", result.err);
    }

    @Test
    void parseArgsParsesRequiredDirectoriesAndOptions() throws Exception {
        CliOptions options = MikuTextBundleCli.parseArgs(new String[] { "--input", ".", "--output", "out",
                "--max-chars", "1000", "--max-input-file-bytes", "2000", "--verbose", "--dry-run" });

        assertEquals(".", options.inputDirectory);
        assertEquals("out", options.outputDirectory);
        assertEquals("text-bundle", options.filenamePrefix);
        assertEquals(1000, options.maxChars);
        assertEquals(2000, options.maxInputFileBytes);
        assertEquals(SupportedEncoding.UTF_8, options.encoding.defaultEncoding);
        assertTrue(options.verbose);
        assertTrue(options.dryRun);
    }

    @Test
    void parseArgsParsesExcludeListOperations() throws Exception {
        CliOptions options = MikuTextBundleCli.parseArgs(new String[] { "--input", ".", "--output", "out",
                "--add-exclude-extension", ".wasm,.BIN", "--remove-exclude-extension", ".pdf",
                "--add-exclude-directory", "generated,./logs/", "--remove-exclude-directory", "dist" });

        assertTrue(options.excludeExtensions.contains(".wasm"));
        assertTrue(options.excludeExtensions.contains(".bin"));
        assertFalse(options.excludeExtensions.contains(".pdf"));
        assertTrue(options.excludeDirectories.contains("generated"));
        assertTrue(options.excludeDirectories.contains("logs"));
        assertFalse(options.excludeDirectories.contains("dist"));
    }

    @Test
    void parseArgsParsesFilenamePrefix() throws Exception {
        CliOptions options = MikuTextBundleCli.parseArgs(new String[] { "--input", ".", "--output", "out",
                "--filename-prefix", "  repo.bundle_1  " });

        assertEquals("repo.bundle_1", options.filenamePrefix);
    }

    @Test
    void invalidFilenamePrefixReturnsUsageError() {
        CliResult empty = run("--input", ".", "--output", "out", "--filename-prefix", "   ");
        CliResult slash = run("--input", ".", "--output", "out", "--filename-prefix", "bad/name");
        CliResult backslash = run("--input", ".", "--output", "out", "--filename-prefix", "bad\\name");
        CliResult newline = run("--input", ".", "--output", "out", "--filename-prefix", "bad\nname");

        assertEquals(1, empty.exitCode);
        assertTrue(empty.err.contains("--filename-prefix must not be empty"));
        assertEquals(1, slash.exitCode);
        assertTrue(slash.err.contains("--filename-prefix must contain only"));
        assertEquals(1, backslash.exitCode);
        assertTrue(backslash.err.contains("--filename-prefix must contain only"));
        assertEquals(1, newline.exitCode);
        assertTrue(newline.err.contains("--filename-prefix must contain only"));
    }

    @Test
    void parseArgsParsesDefaultAndExtensionEncodingOptions() throws Exception {
        CliOptions options = MikuTextBundleCli.parseArgs(new String[] { "--input", ".", "--output", "out",
                "--encoding", "shift_jis", "--encoding-extension", ".ts=utf-8,.java=shift_jis" });

        assertEquals(SupportedEncoding.SHIFT_JIS, options.encoding.defaultEncoding);
        assertEquals(SupportedEncoding.UTF_8, options.encoding.extensions.get(".ts"));
        assertEquals(SupportedEncoding.SHIFT_JIS, options.encoding.extensions.get(".java"));
    }

    @Test
    void invalidEncodingOptionsReturnUsageError() {
        CliResult unsupported = run("--input", ".", "--output", "out", "--encoding", "latin1");
        CliResult missingDot = run("--input", ".", "--output", "out", "--encoding-extension", "java=shift_jis");
        CliResult unsupportedExtension = run("--input", ".", "--output", "out", "--encoding-extension", ".java=latin1");

        assertEquals(1, unsupported.exitCode);
        assertTrue(unsupported.err.contains("--encoding must be one of"));
        assertEquals(1, missingDot.exitCode);
        assertTrue(missingDot.err.contains("leading dot"));
        assertEquals(1, unsupportedExtension.exitCode);
        assertTrue(unsupportedExtension.err.contains("--encoding-extension must be one of"));
    }

    @Test
    void rejectsRemovedAndPositionalArguments() {
        CliResult positional = run(".", "out");
        CliResult oldInput = run("--input-directory", ".", "--output", "out");
        CliResult oldInclude = run("--input", ".", "--output", "out", "--include", "docs/**/*.md");
        CliResult shortHelp = run("-h");

        assertEquals(1, positional.exitCode);
        assertTrue(positional.err.contains("Positional arguments are not supported"));
        assertEquals(1, oldInput.exitCode);
        assertTrue(oldInput.err.contains("Unknown argument: --input-directory"));
        assertEquals(1, oldInclude.exitCode);
        assertTrue(oldInclude.err.contains("Unknown argument: --include"));
        assertEquals(1, shortHelp.exitCode);
        assertTrue(shortHelp.err.contains("Unknown argument: -h"));
    }

    @Test
    void missingRequiredDirectoriesReturnUsageError() {
        CliResult missingInput = run("--output", "out");
        CliResult missingOutput = run("--input", ".");

        assertEquals(1, missingInput.exitCode);
        assertTrue(missingInput.err.contains("Please specify --input."));
        assertEquals(1, missingOutput.exitCode);
        assertTrue(missingOutput.err.contains("Please specify --output."));
    }

    @Test
    void missingOptionValueReturnsUsageError() {
        CliResult result = run("--input");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Please specify a value for --input."));
        assertTrue(result.out.contains("miku-text-bundle --input <dir> --output <dir>"));
    }

    @Test
    void invalidPositiveIntegerReturnsUsageError() {
        CliResult result = run("--input", ".", "--output", "out", "--max-input-file-bytes", "not-a-number");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("--max-input-file-bytes must be a positive integer."));
        assertTrue(result.out.contains("miku-text-bundle --input <dir> --output <dir>"));
    }

    @Test
    void rejectsInvalidExcludeListValues() {
        CliResult extension = run("--input", ".", "--output", "out", "--add-exclude-extension", "png");
        CliResult directory = run("--input", ".", "--output", "out", "--add-exclude-directory", ".");

        assertEquals(1, extension.exitCode);
        assertTrue(extension.err.contains("extensions with a leading dot"));
        assertEquals(1, directory.exitCode);
        assertTrue(directory.err.contains("relative directory names or paths"));
    }

    @Test
    void parseArgsKeepsDefaultExcludeLists() throws Exception {
        CliOptions options = MikuTextBundleCli.parseArgs(new String[] { "--input", ".", "--output", "out" });

        assertTrue(options.excludeExtensions.containsAll(Arrays.asList(".png", ".pdf", ".zip")));
        assertTrue(options.excludeDirectories.containsAll(Arrays.asList(".git", "node_modules", "target")));
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
