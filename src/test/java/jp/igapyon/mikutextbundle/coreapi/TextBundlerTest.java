package jp.igapyon.mikutextbundle.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jp.igapyon.mikutextbundle.model.CliOptions;
import jp.igapyon.mikutextbundle.model.BundleMode;

class TextBundlerTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesNeutralKnowledgeSourcesAndManagementIndex() throws Exception {
        write("docs/guide.md", "# Product\n\nFact A.\n");
        write("src/main.ts", "// TODO implement\nconst value = 1;\n");
        CliOptions options = bundleOptions();
        options.mode = BundleMode.KNOWLEDGE_SOURCE;

        BundleResult result = create(options, new Date(0L));

        assertEquals(BundleMode.KNOWLEDGE_SOURCE, result.mode);
        assertEquals(result.partPaths, result.knowledgeSourcePaths);
        assertTrue(result.partPaths.get(0).endsWith("knowledge-001.md"));
        assertTrue(result.managementIndexPath.endsWith("knowledge-index.md"));
        String knowledge = new String(Files.readAllBytes(java.nio.file.Paths.get(result.partPaths.get(0))), StandardCharsets.UTF_8);
        String index = new String(Files.readAllBytes(java.nio.file.Paths.get(result.managementIndexPath)), StandardCharsets.UTF_8);
        assertTrue(knowledge.contains("- Source path: `docs/guide.md`"));
        assertTrue(knowledge.contains("# Product\n\nFact A."));
        assertFalse(knowledge.contains("Text Bundle Prompt"));
        assertFalse(knowledge.contains("## Markers"));
        assertTrue(index.contains("# Knowledge Bundle Index"));
        assertTrue(index.contains("## Source Mapping"));
        assertTrue(index.contains("TODO implement"));
    }

    @Test
    void tracksSplitKnowledgeSourceRangesAndStaleOutput() throws Exception {
        write("large.md", "aaaa\nbbbb\ncccc\n");
        write("out/knowledge-004.md", "stale\n");
        CliOptions options = bundleOptions();
        options.mode = BundleMode.KNOWLEDGE_SOURCE;
        options.maxChars = 6;

        BundleResult result = create(options, new Date(0L));
        String second = new String(Files.readAllBytes(java.nio.file.Paths.get(result.partPaths.get(1))), StandardCharsets.UTF_8);
        String index = new String(Files.readAllBytes(java.nio.file.Paths.get(result.managementIndexPath)), StandardCharsets.UTF_8);
        assertTrue(second.contains("- Source chunk: 2 / 3"));
        assertTrue(second.contains("- Source lines: 2-2"));
        assertTrue(index.contains("| `large.md` | `knowledge-002.md` | 2 / 3 | 2-2 | 5-10 | 15 | 5 |"));
        assertTrue(result.warnings.contains("Stale generated output remains: `knowledge-004.md`."));
        assertTrue(Files.exists(tempDir.resolve("out/knowledge-004.md")));
    }

    @Test
    void plansKnowledgeSourcesWithoutWritingDuringDryRun() throws Exception {
        write("README.md", "# README\n");
        CliOptions options = bundleOptions();
        options.mode = BundleMode.KNOWLEDGE_SOURCE;
        options.dryRun = true;

        BundleResult result = create(options, new Date(0L));
        assertTrue(result.partPaths.get(0).endsWith("knowledge-001.md"));
        assertTrue(result.managementIndexPath.endsWith("knowledge-index.md"));
        assertFalse(Files.exists(tempDir.resolve("out")));
    }

    @Test
    void choosesExplicitOutputDirectory() {
        Path output = tempDir.resolve("out");

        assertEquals(output.toAbsolutePath().normalize().toString(), TextBundler.chooseOutputDirectory(output.toString()).toString());
    }

    @Test
    void generatesCompactPartFilesWithEmbeddedPromptAndIndexFromDefaultRepositoryFiles() throws Exception {
        write("README.md", "# README\n");
        write("TODO.md", "- TODO root item\n");
        write("src/main.ts", "const value = 1;\n// FIXME check later\n");
        write(".git/secret.ts", "const hidden = true;\n");
        write(".gitignore", "ignored.ts\n");
        write("src/ignored.ts", "const ignored = true;\n");

        BundleResult result = create(bundleOptions(), new Date(1777913520000L));

        assertEquals(4, result.filesCollected);
        assertEquals(2, result.filesIgnored);
        assertEquals(2, result.directoriesIgnored);
        assertEquals(1, result.ignoredByDirectory);
        assertEquals(1, result.ignoredByGitignore);
        assertEquals(0, result.ignoredByOutputDirectory);
        assertEquals(1, result.partsGenerated);
        assertEquals(result.partPaths.get(0), result.promptPath);
        assertEquals(result.partPaths.get(0), result.indexPath);

        String output = read(result.indexPath);
        String index = indexSection(output);
        String part = read(result.partPaths.get(0));
        String prompt = read(result.promptPath);

        assertTrue(part.contains("# Text Bundle Prompt"));
        assertTrue(part.contains("# Text Bundle Part 001"));
        assertTrue(part.contains("# Text Bundle Index"));
        assertTrue(index.contains("`src/main.ts`"));
        assertTrue(index.contains("FIXME"));
        assertTrue(index.contains("`.gitignore`"));
        assertFalse(index.contains("Input directory: `" + tempDir.toAbsolutePath().normalize().toString() + "`"));
        assertFalse(index.contains("Output directory: `" + tempDir.resolve("out").toAbsolutePath().normalize().toString() + "`"));
        assertFalse(index.contains(".git/secret.ts"));
        assertFalse(index.contains("ignored.ts"));
        assertTrue(part.contains("### src/main.ts"));
        assertTrue(part.contains("~~~ts"));
        assertTrue(prompt.contains("text-bundle-001.md"));
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
        String index = indexSection(read(result.indexPath));
        String firstPart = read(result.partPaths.get(0));
        assertTrue(index.contains("--max-chars"));
        assertTrue(firstPart.contains("This file exceeded the size limit and was split."));
        assertTrue(firstPart.indexOf("This file exceeded the size limit and was split.") < firstPart.indexOf("~~~ts"));
    }

    @Test
    void skipsTextFilesThatExceedInputByteLimit() throws Exception {
        write("README.md", "# README\n");
        write("docs/huge.md", repeat("x", 101));
        CliOptions options = bundleOptions();
        options.maxInputFileBytes = 100;

        BundleResult result = create(options, new Date(1777913760000L));

        String index = indexSection(read(result.indexPath));
        String part = partBodySection(read(result.partPaths.get(0)));
        assertEquals(1, result.filesCollected);
        assertEquals(1, result.filesSkipped);
        assertTrue(index.contains("`docs/huge.md`"));
        assertTrue(index.contains("File size exceeds the 100 byte limit."));
        assertFalse(part.contains("docs/huge.md"));
    }

    @Test
    void excludesKnownBinaryExtensionsBeforeReadingFiles() throws Exception {
        write("README.md", "# README\n");
        Path image = tempDir.resolve("assets/image.png");
        Files.createDirectories(image.getParent());
        Files.write(image, new byte[] { 0, 1, 2, 3 });

        BundleResult result = create(bundleOptions(), new Date(1777913700000L));

        String index = indexSection(read(result.indexPath));
        String part = read(result.partPaths.get(0));
        assertTrue(index.contains("`README.md`"));
        assertFalse(index.contains("assets/image.png"));
        assertFalse(part.contains("assets/image.png"));
        assertEquals(0, result.filesSkipped);
        assertEquals(1, result.filesIgnored);
        assertEquals(1, result.ignoredByExtension);
    }

    @Test
    void usesCustomizedExcludeExtensionAndDirectoryLists() throws Exception {
        write("README.md", "# README\n");
        Path pdf = tempDir.resolve("assets/document.pdf");
        Files.createDirectories(pdf.getParent());
        Files.write(pdf, new byte[] { 0, 1, 2, 3 });
        write("notes/skip.md", "# Skip\n");
        write("dist/generated.md", "# Generated\n");
        CliOptions options = bundleOptions();
        options.excludeExtensions = new ArrayList<String>();
        options.excludeDirectories = new ArrayList<String>();
        options.excludeExtensions.add(".png");
        options.excludeDirectories.add("notes");

        BundleResult result = create(options, new Date(1777913700000L));

        String index = indexSection(read(result.indexPath));
        assertTrue(index.contains("`README.md`"));
        assertTrue(index.contains("`dist/generated.md`"));
        assertTrue(index.contains("`assets/document.pdf`"));
        assertTrue(index.contains("UTF-8"));
        assertFalse(index.contains("notes/skip.md"));
        assertEquals(1, result.filesIgnored);
        assertEquals(2, result.directoriesIgnored);
        assertEquals(1, result.ignoredByDirectory);
        assertEquals(0, result.ignoredByOutputDirectory);
    }

    @Test
    void writesIndexMarkdownSectionsInStableOrder() throws Exception {
        write("README.md", "# README\n");
        write("TODO.md", "- TODO check index\n");
        write("src/main.ts", "const value = 1;\n");
        Path path = tempDir.resolve("src/bad.ts");
        Files.write(path, new byte[] { 0 });

        BundleResult result = create(bundleOptions(), new Date(1777913820000L));

        String index = indexSection(read(result.indexPath));
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

        String index = indexSection(read(result.indexPath));
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
        assertTrue(prompt.contains("## Reading Order"));
        assertTrue(prompt.contains("1. `text-bundle-001.md`"));
        assertFalse(prompt.contains("2. `text-bundle-001.md`"));
        assertTrue(prompt.contains("`OK`"));
        assertFalse(prompt.contains("`END_OF_TEXT_BUNDLE`"));
        assertTrue(prompt.contains("## Response File"));
        assertTrue(prompt.contains("`text-bundle-response.md`"));
        assertTrue(prompt.contains("## Output Format"));
        assertTrue(prompt.contains("~~~~"));
    }

    @Test
    void usesCustomFilenamePrefixForGeneratedBundleFiles() throws Exception {
        write("README.md", "# README\n");
        write("src/main.ts", "const value = 1;\n");
        CliOptions options = bundleOptions();
        options.filenamePrefix = "sample-repo-text-bundle";

        BundleResult result = create(options, new Date(1777913880000L));

        assertTrue(result.promptPath.endsWith("sample-repo-text-bundle-001.md"));
        assertTrue(result.partPaths.get(0).endsWith("sample-repo-text-bundle-001.md"));
        assertTrue(result.indexPath.endsWith("sample-repo-text-bundle-001.md"));

        String prompt = read(result.promptPath);
        String index = read(result.indexPath);
        assertTrue(prompt.contains("1. `sample-repo-text-bundle-001.md`"));
        assertFalse(prompt.contains("2. `sample-repo-text-bundle-001.md`"));
        assertTrue(index.contains("| `sample-repo-text-bundle-001.md` |"));
    }

    @Test
    void rejectsUnsafeFilenamePrefixesThroughCoreApi() throws Exception {
        write("README.md", "# README\n");
        CliOptions options = bundleOptions();
        options.filenamePrefix = "bad/name";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> create(options, new Date(1777913880000L)));

        assertTrue(exception.getMessage().contains("filenamePrefix must contain only"));
    }

    @Test
    void ordersBundleFilesByPosixRelativePathUtf16CodeUnits() throws Exception {
        write("file-2.txt", "two\n");
        write("file-10.txt", "ten\n");
        write("A.txt", "upper\n");
        write("b.txt", "lower\n");
        write("あ.txt", "hiragana\n");

        BundleResult result = create(bundleOptions(), new Date(1777913880000L));

        String part = read(result.partPaths.get(0));
        assertTrue(part.indexOf("### A.txt") < part.indexOf("### b.txt"));
        assertTrue(part.indexOf("### b.txt") < part.indexOf("### file-10.txt"));
        assertTrue(part.indexOf("### file-10.txt") < part.indexOf("### file-2.txt"));
        assertTrue(part.indexOf("### file-2.txt") < part.indexOf("### あ.txt"));
    }

    @Test
    void allowsTextBundle999AsFinalCompactPart() throws Exception {
        for (int index = 1; index <= 999; index++) {
            write("src/file-" + String.format("%03d", index) + ".txt", "x");
        }
        CliOptions options = bundleOptions();
        options.maxChars = 1;

        BundleResult result = create(options, new Date(1777913880000L));

        assertTrue(result.partPaths.get(result.partPaths.size() - 1).endsWith("text-bundle-999.md"));
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
        assertTrue(part.contains("~~~ts\nconst value = 1;\n\n~~~"));
    }

    @Test
    void separatesLaterFileChunksWithHorizontalRule() throws Exception {
        write("a.txt", "a\n");
        write("b.txt", "b\n");

        BundleResult result = create(bundleOptions(), new Date(1777913940000L));

        String part = read(result.partPaths.get(0));
        assertTrue(part.contains("~~~\n\n---\n\n### b.txt"));
    }

    @Test
    void addsAcknowledgementOnlyFooterToNonTerminalParts() throws Exception {
        write("a.txt", "a\n");
        write("b.txt", "b\n");
        CliOptions options = bundleOptions();
        options.maxChars = 2;

        BundleResult result = create(options, new Date(1777913940000L));

        assertEquals(2, result.partsGenerated);
        String firstPart = read(result.partPaths.get(0));
        String finalPart = read(result.partPaths.get(1));
        assertTrue(firstPart.contains("## Acknowledgement"));
        assertTrue(firstPart.contains("Reply only with `OK`."));
        assertFalse(finalPart.contains("## Acknowledgement"));
        assertTrue(finalPart.contains("# Text Bundle Index"));
    }

    @Test
    void doesNotCreateOutputFilesInDryRunMode() throws Exception {
        write("README.md", "# README\n");
        write("src/main.ts", "const value = 1;\n");
        CliOptions options = bundleOptions();
        options.dryRun = true;

        BundleResult result = create(options, new Date(1777913940000L));

        assertTrue(result.dryRun);
        assertEquals(2, result.filesCollected);
        assertEquals(1, result.partsGenerated);
        assertEquals(tempDir.resolve("out").resolve("text-bundle-001.md").toAbsolutePath().normalize().toString(),
                result.partPaths.get(0));
        assertFalse(Files.exists(tempDir.resolve("out")));
    }

    @Test
    void doesNotImposeRenderedMarkdownCharacterLimitWhenSmallFileOverheadAccumulates() throws Exception {
        for (int index = 1; index <= 1800; index++) {
            write("src/module-" + String.format("%04d", index) + ".ts", "export const value" + index + " = " + index + ";\n");
        }

        BundleResult result = create(bundleOptions(), new Date(1777913940000L));

        assertEquals(1, result.partsGenerated);
        assertTrue(read(result.partPaths.get(0)).length() > 128000);
    }

    @Test
    void generatesStableBundleFromProductFixture() throws Exception {
        copyResourceDirectory("fixtures/product-repo", tempDir);
        CliOptions options = bundleOptions();

        BundleResult result = create(options, new Date(1777914060000L));

        assertEquals(6, result.filesCollected);
        assertEquals(0, result.filesIgnored);
        assertEquals(0, result.filesSkipped);
        assertEquals(1, result.partsGenerated);

        String index = indexSection(read(result.indexPath));
        String part = read(result.partPaths.get(0));

        assertTrue(index.contains("| `text-bundle-001.md` | 6 |"));
        assertTrue(index.contains("`docs/extra.md`"));
        assertTrue(index.contains("`docs/skip.md`"));
        assertTrue(index.contains("| `src/main.ts` | 2 | TODO | // TODO: stabilize fixture behavior |"));

        assertTrue(part.indexOf("### README.md") < part.indexOf("### TODO.md"));
        assertTrue(part.indexOf("### TODO.md") < part.indexOf("### docs/extra.md"));
        assertTrue(part.indexOf("### docs/extra.md") < part.indexOf("### docs/skip.md"));
        assertTrue(part.indexOf("### docs/skip.md") < part.indexOf("### src/Alpha.java"));
        assertTrue(part.indexOf("### src/Alpha.java") < part.indexOf("### src/main.ts"));
        assertTrue(part.contains("~~~java\npackage fixture;\n\npublic final class Alpha {\n}\n\n~~~"));
    }

    private BundleResult create(CliOptions options, Date now) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        return new TextBundler().createTextBundle(options, now, new PrintStream(out));
    }

    private String indexSection(String content) {
        int indexStart = content.indexOf("# Text Bundle Index");
        return indexStart < 0 ? content : content.substring(indexStart);
    }

    private String partBodySection(String content) {
        int indexStart = content.indexOf("# Text Bundle Index");
        return indexStart < 0 ? content : content.substring(0, indexStart);
    }

    private CliOptions bundleOptions() {
        CliOptions options = new CliOptions();
        options.inputDirectory = tempDir.toString();
        options.outputDirectory = tempDir.resolve("out").toString();
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
