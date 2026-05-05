package jp.igapyon.mikutextbundle.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class MikuTextBundleCliTest {
    @Test
    void helpReturnsUsage() {
        CliResult result = run("--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Usage: miku-text-bundle-java"));
        assertEquals("", result.err);
    }

    @Test
    void versionReturnsProductVersion() {
        CliResult result = run("--version");

        assertEquals(0, result.exitCode);
        assertEquals("miku-text-bundle-java 0.1.0-SNAPSHOT\n", result.out);
        assertEquals("", result.err);
    }

    @Test
    void unknownCommandReturnsUsageError() {
        CliResult result = run("bundle");

        assertEquals(2, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("Unknown option or command: bundle"));
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
