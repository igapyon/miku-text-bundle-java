package jp.igapyon.mikutextbundle.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikutextbundle.model.BundleChunk;
import jp.igapyon.mikutextbundle.model.BundlePart;
import jp.igapyon.mikutextbundle.model.CollectedFile;
import jp.igapyon.mikutextbundle.model.Marker;
import jp.igapyon.mikutextbundle.model.SkippedFile;

class MarkdownTest {
    @Test
    void buildsStablePartMarkdown() {
        assertEquals("---\n" +
                "tool: miku-text-bundle\n" +
                "version: 1.6.0\n" +
                "role: part\n" +
                "part: 1\n" +
                "---\n" +
                "\n" +
                "# Text Bundle Part 001\n" +
                "\n" +
                "- Part file: `text-bundle-001.md`\n" +
                "- Files/chunks: 1\n" +
                "- Approx chars: 17\n" +
                "\n" +
                "### FILE: src/main.ts\n" +
                "\n" +
                "--- BEGIN FILE: src/main.ts ---\n" +
                "\n" +
                "Source code block\n" +
                "Language: TypeScript\n" +
                "\n" +
                "~~~ts\n" +
                "const value = 1;\n" +
                "~~~\n" +
                "\n" +
                "--- END FILE: src/main.ts ---\n" +
                "\n",
                Markdown.buildPartMarkdown(part()));
    }

    @Test
    void rendersJavaScriptWithExplicitAgentReadableFileBlockMetadata() {
        BundlePart part = part();
        BundleChunk chunk = part.chunks.get(0);
        chunk.relativePath = "src/example.js";
        chunk.extension = "js";
        chunk.content = "function hello() {\n  return \"hello\";\n}\n";

        String markdown = Markdown.buildPartMarkdown(part);
        org.junit.jupiter.api.Assertions.assertTrue(markdown.contains("### FILE: src/example.js\n\n" +
                "--- BEGIN FILE: src/example.js ---\n\n" +
                "Source code block\nLanguage: JavaScript\n\n" +
                "~~~js\nfunction hello() {\n  return \"hello\";\n}\n~~~\n\n" +
                "--- END FILE: src/example.js ---"));
    }

    @Test
    void mapsCommonSourceAndStructuredTextExtensionsToExplicitLanguages() {
        assertLanguage("src/app.py", "py", "Source code block", "Python", "python");
        assertLanguage("src/main.go", "go", "Source code block", "Go", "go");
        assertLanguage("config/settings.yaml", "yaml", "Source text block", "YAML", "yaml");
        assertLanguage("web/index.html", "html", "Source code block", "HTML", "html");
        assertLanguage("Dockerfile", "", "Source code block", "Dockerfile", "dockerfile");
    }

    @Test
    void usesNeutralMetadataForUnknownExtensions() {
        BundlePart part = part();
        part.chunks.get(0).relativePath = "data/example.custom-format";
        part.chunks.get(0).extension = "custom-format";

        org.junit.jupiter.api.Assertions.assertTrue(Markdown.buildPartMarkdown(part)
                .contains("Source content block\nLanguage: Unknown\n\n~~~\n"));
    }

    @Test
    void preservesRepeatedBlankLinesInsideFileBodiesInBothModes() {
        BundlePart part = part();
        String content = "first\n\n\nsecond\n";
        part.chunks.get(0).content = content;

        org.junit.jupiter.api.Assertions.assertTrue(Markdown.buildPartMarkdown(part).contains(content));
        org.junit.jupiter.api.Assertions.assertTrue(Markdown.buildKnowledgeSourceMarkdown(part).contains(content));
    }

    @Test
    void escapesControlCharactersInFileBlockDisplayPaths() {
        BundlePart part = part();
        part.chunks.get(0).relativePath = "docs/line\nbreak\tname.md";
        part.chunks.get(0).extension = "md";
        String markdown = Markdown.buildPartMarkdown(part);
        org.junit.jupiter.api.Assertions.assertTrue(markdown.contains(
                "### FILE: docs/line\\nbreak\\tname.md\n\n--- BEGIN FILE: docs/line\\nbreak\\tname.md ---"));

        part.chunks.get(0).relativePath = "docs/literal\\n.md";
        org.junit.jupiter.api.Assertions.assertTrue(Markdown.buildPartMarkdown(part)
                .contains("### FILE: docs/literal\\\\n.md"));
    }

    @Test
    void keepsControlCharactersPipesAndBackticksInsideIndexPathCells() {
        BundlePart part = part();
        part.chunks.get(0).relativePath = "docs/line\nbreak|`name`.md";
        String index = Markdown.buildIndexMarkdown("/repo", "/out", Arrays.asList(part),
                collectedFiles(), skippedFiles(), markers(), new ArrayList<String>());

        org.junit.jupiter.api.Assertions.assertTrue(index.contains("``docs/line\\nbreak\\|`name`.md``"));
        org.junit.jupiter.api.Assertions.assertFalse(index.contains("docs/line\nbreak"));
    }

    @Test
    void buildsStableKnowledgeSourceFileBlockMarkdown() {
        BundlePart part = part();
        part.fileName = "knowledge-001.md";
        BundleChunk chunk = part.chunks.get(0);
        chunk.relativePath = "docs/guide.md";
        chunk.extension = "md";
        chunk.content = "# Guide\n\nDetails.\n";

        assertEquals("# Knowledge Source 001\n" +
                "\n" +
                "### FILE: docs/guide.md\n" +
                "\n" +
                "--- BEGIN FILE: docs/guide.md ---\n" +
                "\n" +
                "Source text block\n" +
                "Language: Markdown\n" +
                "\n" +
                "~~~md\n" +
                "# Guide\n" +
                "\n" +
                "Details.\n" +
                "~~~\n" +
                "\n" +
                "--- END FILE: docs/guide.md ---\n" +
                "\n",
                Markdown.buildKnowledgeSourceMarkdown(part));
    }

    @Test
    void usesLongerTildeFencesWhenContentContainsTildeFences() {
        BundlePart part = part();
        part.chunks.get(0).content = "~~~md\ninside\n~~~\n";

        org.junit.jupiter.api.Assertions.assertTrue(Markdown.buildPartMarkdown(part)
                .contains("~~~~ts\n~~~md\ninside\n~~~\n~~~~"));
    }

    @Test
    void wrapsEveryFileChunkInExplicitFileBoundaryMarkers() {
        BundlePart part = part();
        BundleChunk second = new BundleChunk();
        second.relativePath = "docs/guide/setup.md";
        second.extension = "md";
        second.content = "# Setup\n";
        second.originalCharCount = 8;
        second.originalLineCount = 2;
        second.chunkIndex = 1;
        second.chunkCount = 1;
        part.chunks.add(second);
        part.charCount = 25;

        org.junit.jupiter.api.Assertions.assertTrue(Markdown.buildPartMarkdown(part)
                .contains("--- END FILE: src/main.ts ---\n\n### FILE: docs/guide/setup.md\n\n" +
                        "--- BEGIN FILE: docs/guide/setup.md ---"));
    }

    @Test
    void addsAcknowledgementFooterWhenRequested() {
        String markdown = Markdown.buildPartMarkdown(part(), null, null, true);

        org.junit.jupiter.api.Assertions.assertTrue(markdown.contains("## Acknowledgement"));
        org.junit.jupiter.api.Assertions.assertTrue(markdown.contains("Reply only with `OK`."));
    }

    @Test
    void buildsStableIndexMarkdown() {
        assertEquals("---\n" +
                "tool: miku-text-bundle\n" +
                "version: 1.6.0\n" +
                "role: index\n" +
                "terminal: true\n" +
                "---\n" +
                "\n" +
                "# Text Bundle Index\n" +
                "\n" +
                "## Summary\n" +
                "\n" +
                "- Input directory: `/repo`\n" +
                "- Output directory: `/repo/workplace/miku-text-bundle/202605051200`\n" +
                "- Collected files: 1\n" +
                "- Skipped files: 1\n" +
                "- Parts: 1\n" +
                "\n" +
                "## Parts\n" +
                "\n" +
                "| Part | Chunks | Approx chars | Files |\n" +
                "| --- | ---: | ---: | --- |\n" +
                "| `text-bundle-001.md` | 1 | 17 | `src/main.ts` |\n" +
                "\n" +
                "## Skipped Files\n" +
                "\n" +
                "| File | Reason |\n" +
                "| --- | --- |\n" +
                "| `docs/huge.md` | File size exceeds the 100 byte limit. |\n" +
                "\n" +
                "## Warnings\n" +
                "\n" +
                "- `src/large.ts` exceeded --max-chars and was split into 2 chunks.\n" +
                "\n" +
                "## Markers\n" +
                "\n" +
                "| File | Line | Kind | Text |\n" +
                "| --- | ---: | --- | --- |\n" +
                "| `TODO.md` | 3 | TODO | - TODO check \\| escape |\n" +
                "\n",
                Markdown.buildIndexMarkdown("/repo", "/repo/workplace/miku-text-bundle/202605051200",
                        Arrays.asList(part()), collectedFiles(), skippedFiles(), markers(),
                        Arrays.asList("`src/large.ts` exceeded --max-chars and was split into 2 chunks.")));
    }

    @Test
    void addsAgentSkillHandoffGuidanceWhenSkillMarkdownIsBundled() {
        CollectedFile skillFile = new CollectedFile();
        skillFile.absolutePath = "/repo/skills/example/SKILL.md";
        skillFile.relativePath = "skills/example/SKILL.md";
        skillFile.extension = "md";
        skillFile.content = "---\nname: example\n---\n";
        skillFile.charCount = 22;
        skillFile.lineCount = 3;

        List<CollectedFile> files = new ArrayList<CollectedFile>(collectedFiles());
        files.add(skillFile);

        String index = Markdown.buildIndexMarkdown("/repo", "/repo/workplace/miku-text-bundle/202605051200",
                Arrays.asList(part()), files, skippedFiles(), markers(),
                Arrays.asList("`src/large.ts` exceeded --max-chars and was split into 2 chunks."));

        org.junit.jupiter.api.Assertions.assertTrue(index.contains("## Agent Skill Handoff"));
        org.junit.jupiter.api.Assertions.assertTrue(index.contains("`skills/example/SKILL.md`"));
        org.junit.jupiter.api.Assertions.assertTrue(index.contains("keep them available for reference in this conversation"));
        org.junit.jupiter.api.Assertions.assertTrue(index.contains("activation rules, operating rules, workflow, and references"));
    }

    @Test
    void buildsStablePromptMarkdown() {
        assertEquals("---\n" +
                "tool: miku-text-bundle\n" +
                "version: 1.6.0\n" +
                "role: prompt\n" +
                "---\n" +
                "\n" +
                "# Text Bundle Prompt\n" +
                "\n" +
                "This is the reading instruction for a Text Bundle that packages a set of files for handoff to generative AI or similar tools.\n" +
                "\n" +
                "The Markdown bundle will be sent in multiple messages in the order listed below.\n" +
                "\n" +
                "After each non-terminal Part, do not analyze or summarize the content yet. Reply only with `OK`.\n" +
                "\n" +
                "Do not start the final response until you receive `text-bundle-002.md`.\n" +
                "\n" +
                "## Reading Order\n" +
                "\n" +
                "1. `text-bundle-001.md`\n" +
                "2. `text-bundle-002.md`\n" +
                "\n" +
                "## Response File\n" +
                "\n" +
                "If you save the final response after `text-bundle-002.md`, `text-bundle-response.md` is the recommended filename.\n" +
                "\n" +
                "## Output Format\n" +
                "\n" +
                "Output the final response as Markdown text.\n" +
                "\n" +
                "Wrap the entire final Markdown response in a single outer fence using `~~~~`. Use tildes for the outer fence because the Markdown response may contain backtick code fences.\n",
                Markdown.buildPromptMarkdown(Arrays.asList("text-bundle-001.md", "text-bundle-002.md")));
    }

    private BundlePart part() {
        BundleChunk chunk = new BundleChunk();
        chunk.relativePath = "src/main.ts";
        chunk.extension = "ts";
        chunk.content = "const value = 1;\n";
        chunk.originalCharCount = 17;
        chunk.originalLineCount = 2;
        chunk.chunkIndex = 1;
        chunk.chunkCount = 1;

        BundlePart part = new BundlePart();
        part.fileName = "text-bundle-001.md";
        part.partNumber = 1;
        part.charCount = 17;
        part.chunks.add(chunk);
        return part;
    }

    private void assertLanguage(String relativePath, String extension, String blockLabel, String displayName,
            String fenceLanguage) {
        BundlePart part = part();
        part.chunks.get(0).relativePath = relativePath;
        part.chunks.get(0).extension = extension;
        org.junit.jupiter.api.Assertions.assertTrue(Markdown.buildPartMarkdown(part)
                .contains(blockLabel + "\nLanguage: " + displayName + "\n\n~~~" + fenceLanguage));
    }

    private List<CollectedFile> collectedFiles() {
        CollectedFile file = new CollectedFile();
        file.absolutePath = "/repo/src/main.ts";
        file.relativePath = "src/main.ts";
        file.extension = "ts";
        file.content = "const value = 1;\n";
        file.charCount = 17;
        file.lineCount = 2;
        return Arrays.asList(file);
    }

    private List<SkippedFile> skippedFiles() {
        SkippedFile file = new SkippedFile();
        file.relativePath = "docs/huge.md";
        file.reason = "File size exceeds the 100 byte limit.";
        return Arrays.asList(file);
    }

    private List<Marker> markers() {
        Marker marker = new Marker();
        marker.relativePath = "TODO.md";
        marker.line = 3;
        marker.kind = "TODO";
        marker.text = "- TODO check | escape";
        List<Marker> markers = new ArrayList<Marker>();
        markers.add(marker);
        return markers;
    }
}
