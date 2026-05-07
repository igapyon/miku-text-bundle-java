package jp.igapyon.mikutextbundle.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class TextBundlerTest {
    @TempDir
    Path tempDir;

    @Test
    void choosesTimestampedOutputDirectoryAndSuffixesCollisions() throws Exception {
        Date now = new Date(1777913520000L);
        Path base = TextBundler.defaultOutputBase(tempDir, now);
        Files.createDirectories(base);

        assertEquals(base.toString() + "-1", TextBundler.chooseOutputDirectory(tempDir, null, now).toString());
    }

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
    void usesExtensionEncodingRulesForShiftJisFiles() throws Exception {
        write("src/Legacy.java", "こんにちは\n", java.nio.charset.Charset.forName("Shift_JIS"));
        CliOptions options = bundleOptions();
        options.encoding.extensions.put(".java", jp.igapyon.mikutextbundle.model.SupportedEncoding.SHIFT_JIS);

        BundleResult result = create(options, new Date(1777913880000L));

        String part = read(result.partPaths.get(0));
        assertEquals(1, result.filesCollected);
        assertEquals(0, result.filesSkipped);
        assertTrue(part.contains("こんにちは"));
    }

    @Test
    void usesDefaultEncodingWhenNoExtensionRuleMatches() throws Exception {
        write("README.md", "# 説明\n", java.nio.charset.Charset.forName("Shift_JIS"));
        CliOptions options = bundleOptions();
        options.encoding.defaultEncoding = jp.igapyon.mikutextbundle.model.SupportedEncoding.SHIFT_JIS;

        BundleResult result = create(options, new Date(1777913940000L));

        String part = read(result.partPaths.get(0));
        assertEquals(1, result.filesCollected);
        assertEquals(0, result.filesSkipped);
        assertTrue(part.contains("# 説明"));
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

    @Test
    void skipsExplicitlyIncludedFilesThatExceedInputByteLimit() throws Exception {
        write("README.md", "# README\n");
        write("docs/huge.md", repeat("x", 101));
        CliOptions options = bundleOptions();
        options.maxInputFileBytes = 100;
        options.includePatterns.add("docs/**/*.md");

        BundleResult result = create(options, new Date(1777913760000L));

        String index = read(result.indexPath);
        String part = read(result.partPaths.get(0));
        assertEquals(1, result.filesCollected);
        assertEquals(1, result.filesSkipped);
        assertTrue(index.contains("`docs/huge.md`"));
        assertTrue(index.contains("ファイルサイズ"));
        assertFalse(part.contains("docs/huge.md"));
    }

    @Test
    void honorsIncludeAndExcludePatternsWithoutBypassingHardExclusions() throws Exception {
        write("README.md", "# README\n");
        write("docs/extra.md", "# Extra\n");
        write("docs/skip.md", "# Skip\n");
        write("src/main.ts", "const value = 1;\n");
        write(".secret/extra.md", "# Secret\n");
        write(".gitignore", "ignored.md\n");
        write("docs/ignored.md", "# Ignored\n");
        CliOptions options = bundleOptions();
        options.includePatterns.add("docs/**/*.md");
        options.includePatterns.add(".secret/**/*.md");
        options.excludePatterns.add("docs/skip.md");

        BundleResult result = create(options, new Date(1777913700000L));

        String index = read(result.indexPath);
        String part = read(result.partPaths.get(0));
        assertTrue(index.contains("`README.md`"));
        assertTrue(index.contains("`docs/extra.md`"));
        assertFalse(index.contains("docs/skip.md"));
        assertFalse(index.contains("docs/ignored.md"));
        assertFalse(index.contains(".secret"));
        assertTrue(part.indexOf("### docs/extra.md") < part.indexOf("### README.md"));
        assertTrue(part.indexOf("### README.md") < part.indexOf("### src/main.ts"));
    }

    @Test
    void writesIndexMarkdownSectionsInStableOrder() throws Exception {
        write("README.md", "# README\n");
        write("TODO.md", "- TODO check index\n");
        write("src/main.ts", "const value = 1;\n");
        Path path = tempDir.resolve("src/bad.ts");
        Files.write(path, new byte[] { 0 });

        BundleResult result = create(bundleOptions(), new Date(1777913820000L));

        String index = read(result.indexPath);
        assertTrue(index.contains("# Text Bundle Index\n"));
        assertTrue(index.indexOf("## Summary") < index.indexOf("## Parts"));
        assertTrue(index.indexOf("## Parts") < index.indexOf("## Skipped Files"));
        assertTrue(index.indexOf("## Skipped Files") < index.indexOf("## Warnings"));
        assertTrue(index.indexOf("## Warnings") < index.indexOf("## Markers"));
        assertTrue(index.contains("| Part | Chunks | Approx chars | Files |"));
        assertTrue(index.contains("| File | Reason |"));
        assertTrue(index.contains("| File | Line | Kind | Text |"));
    }

    @Test
    void doesNotExtractMarkersFromFilenameReferences() throws Exception {
        write("README.md", "See TODO.md for project tasks.\nTODO: actionable item\n");

        BundleResult result = create(bundleOptions(), new Date(1777914000000L));

        String index = read(result.indexPath);
        assertTrue(index.contains("TODO: actionable item"));
        assertFalse(index.contains("See TODO.md for project tasks."));
    }

    @Test
    void writesPromptReadingOrderAndResponseContract() throws Exception {
        write("README.md", "# README\n");
        write("src/main.ts", "const value = 1;\n");

        BundleResult result = create(bundleOptions(), new Date(1777913880000L));

        String prompt = read(result.promptPath);
        assertTrue(prompt.contains("# Text Bundle Prompt\n"));
        assertTrue(prompt.contains("## 読み込み順"));
        assertTrue(prompt.contains("1. `text-bundle-000-index.md`"));
        assertTrue(prompt.contains("2. `text-bundle-001.md`"));
        assertTrue(prompt.contains("`受領しました`"));
        assertTrue(prompt.contains("`END_OF_TEXT_BUNDLE`"));
        assertTrue(prompt.contains("## 回答ファイル"));
        assertTrue(prompt.contains("`text-bundle-response.md`"));
        assertTrue(prompt.contains("## 出力形式"));
        assertTrue(prompt.contains("~~~~"));
    }

    @Test
    void writesPartMarkdownWithPathHeadingsAndBacktickCodeFences() throws Exception {
        write("src/main.ts", "const value = 1;\n");

        BundleResult result = create(bundleOptions(), new Date(1777913940000L));

        String part = read(result.partPaths.get(0));
        assertTrue(part.contains("# Text Bundle Part 001"));
        assertTrue(part.contains("### src/main.ts"));
        assertTrue(part.contains("- Characters: 17"));
        assertTrue(part.contains("- Source characters: 17"));
        assertTrue(part.contains("- Source lines: 2"));
        assertTrue(part.contains("```ts\nconst value = 1;\n\n```"));
    }

    @Test
    void generatesStableBundleFromProductFixture() throws Exception {
        copyResourceDirectory("fixtures/product-repo", tempDir);
        CliOptions options = bundleOptions();
        options.includePatterns.add("docs/**/*.md");
        options.excludePatterns.add("docs/skip.md");

        BundleResult result = create(options, new Date(1777914060000L));

        assertEquals(5, result.filesCollected);
        assertEquals(0, result.filesSkipped);
        assertEquals(1, result.partsGenerated);

        String index = read(result.indexPath);
        String part = read(result.partPaths.get(0));

        assertTrue(index.contains("| `text-bundle-001.md` | 5 |"));
        assertTrue(index.contains("`docs/extra.md`"));
        assertFalse(index.contains("docs/skip.md"));
        assertTrue(index.contains("| `src/main.ts` | 2 | TODO | // TODO: stabilize fixture behavior |"));

        assertTrue(part.indexOf("### docs/extra.md") < part.indexOf("### README.md"));
        assertTrue(part.indexOf("### README.md") < part.indexOf("### src/Alpha.java"));
        assertTrue(part.indexOf("### src/Alpha.java") < part.indexOf("### src/main.ts"));
        assertTrue(part.indexOf("### src/main.ts") < part.indexOf("### TODO.md"));
        assertTrue(part.contains("```java\npackage fixture;\n\npublic final class Alpha {\n}\n\n```"));
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
        write(relativePath, content, StandardCharsets.UTF_8);
    }

    private void write(String relativePath, String content, java.nio.charset.Charset charset) throws IOException {
        Path path = tempDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(charset));
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

    private String read(String path) throws IOException {
        return new String(Files.readAllBytes(java.nio.file.Paths.get(path)), StandardCharsets.UTF_8);
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
