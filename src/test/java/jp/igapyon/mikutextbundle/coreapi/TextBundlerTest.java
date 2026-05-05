package jp.igapyon.mikutextbundle.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jp.igapyon.mikutextbundle.model.CliOptions;

class TextBundlerTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesIndexPartsAndPromptFromDefaultRepositoryFiles() throws Exception {
        write("README.md", "# README\n");
        write("TODO.md", "- TODO root item\n");
        write("src/main.ts", "const value = 1;\n// FIXME check later\n");
        write(".hidden/secret.ts", "const hidden = true;\n");
        write(".gitignore", "ignored.ts\n");
        write("src/ignored.ts", "const ignored = true;\n");

        BundleResult result = create(bundleOptions(), new Date(1777913520000L));

        assertEquals(3, result.filesCollected);
        assertEquals(1, result.partsGenerated);

        String index = read(result.indexPath);
        String part = read(result.partPaths.get(0));
        String prompt = read(result.promptPath);

        assertTrue(index.contains("`src/main.ts`"));
        assertTrue(index.contains("FIXME"));
        assertFalse(index.contains(".hidden"));
        assertFalse(index.contains("ignored.ts"));
        assertTrue(part.contains("### src/main.ts"));
        assertTrue(part.contains("```ts"));
        assertTrue(prompt.contains("text-bundle-000-index.md"));
        assertTrue(prompt.contains("text-bundle-response.md"));
    }

    @Test
    void skipsBinaryFilesAndRecordsReason() throws Exception {
        Path path = tempDir.resolve("src/main.ts");
        Files.createDirectories(path.getParent());
        Files.write(path, new byte[] { 0, 1, 2, 3 });

        BundleResult result = create(bundleOptions(), new Date(1777913580000L));

        assertEquals(1, result.filesSkipped);
        assertTrue(read(result.indexPath).contains("UTF-8"));
    }

    @Test
    void splitsOversizedFilesAndWritesWarningsOutsideCodeFences() throws Exception {
        write("src/large.ts", "line1\nline2\nline3\nline4\n");
        CliOptions options = bundleOptions();
        options.maxChars = 12;

        BundleResult result = create(options, new Date(1777913640000L));

        assertTrue(result.partsGenerated > 1);
        String index = read(result.indexPath);
        String firstPart = read(result.partPaths.get(0));
        assertTrue(index.contains("--max-chars"));
        assertTrue(firstPart.contains("やむを得ず分割"));
        assertTrue(firstPart.indexOf("やむを得ず分割") < firstPart.indexOf("```ts"));
    }

    private BundleResult create(CliOptions options, Date now) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        return new TextBundler().createTextBundle(options, now, new PrintStream(out));
    }

    private CliOptions bundleOptions() {
        CliOptions options = new CliOptions();
        options.inputDirectory = tempDir.toString();
        return options;
    }

    private void write(String relativePath, String content) throws IOException {
        Path path = tempDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private String read(String path) throws IOException {
        return new String(Files.readAllBytes(java.nio.file.Paths.get(path)), StandardCharsets.UTF_8);
    }
}
