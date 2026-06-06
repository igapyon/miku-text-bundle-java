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
        assertEquals("# Text Bundle Part 001\n" +
                "\n" +
                "- Part file: `text-bundle-001.md`\n" +
                "- Files/chunks: 1\n" +
                "- Approx chars: 17\n" +
                "\n" +
                "### src/main.ts\n" +
                "\n" +
                "- Characters: 17\n" +
                "- Source characters: 17\n" +
                "- Source lines: 2\n" +
                "\n" +
                "```ts\n" +
                "const value = 1;\n" +
                "\n" +
                "```\n" +
                "\n",
                Markdown.buildPartMarkdown(part()));
    }

    @Test
    void buildsStableIndexMarkdown() {
        assertEquals("# Text Bundle Index\n" +
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
                "| `docs/huge.md` | ファイルサイズが 100 bytes の上限を超えたためスキップしました。 |\n" +
                "\n" +
                "## Warnings\n" +
                "\n" +
                "- `src/large.ts` は --max-chars を超えたため 2 個に分割しました。\n" +
                "\n" +
                "## Markers\n" +
                "\n" +
                "| File | Line | Kind | Text |\n" +
                "| --- | ---: | --- | --- |\n" +
                "| `TODO.md` | 3 | TODO | - TODO check \\| escape |\n" +
                "\n",
                Markdown.buildIndexMarkdown("/repo", "/repo/workplace/miku-text-bundle/202605051200",
                        Arrays.asList(part()), collectedFiles(), skippedFiles(), markers(),
                        Arrays.asList("`src/large.ts` は --max-chars を超えたため 2 個に分割しました。")));
    }

    @Test
    void buildsStablePromptMarkdown() {
        assertEquals("# Text Bundle Prompt\n" +
                "\n" +
                "これから Markdown バンドルを複数のメッセージに分けて順番に送ります。\n" +
                "\n" +
                "各メッセージを受け取ったら、内容の分析や要約はまだ行わず、`受領しました` とだけ返してください。\n" +
                "\n" +
                "`text-bundle-999-index.md` を受け取るまで、最終回答を開始しないでください。\n" +
                "\n" +
                "## 読み込み順\n" +
                "\n" +
                "1. `text-bundle-000-prompt.md`\n" +
                "2. `text-bundle-001.md`\n" +
                "3. `text-bundle-002.md`\n" +
                "4. `text-bundle-999-index.md`\n" +
                "\n" +
                "## 回答ファイル\n" +
                "\n" +
                "`text-bundle-999-index.md` の後に作成する回答は `text-bundle-response.md` として保存する想定です。\n" +
                "\n" +
                "## 出力形式\n" +
                "\n" +
                "markdown テキスト形式で出力してください。\n" +
                "\n" +
                "○最終的な回答は Markdown テキスト形式で出力し、さらに ~~~~ で囲まれた一塊として出力してください。markdown 内に backtick による code fence が含まれる場合があるため、外側の囲みは tilde を使ってください。\n",
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
        file.reason = "ファイルサイズが 100 bytes の上限を超えたためスキップしました。";
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
