package jp.igapyon.mikutextbundle.markdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikutextbundle.model.BundleChunk;
import jp.igapyon.mikutextbundle.model.BundlePart;
import jp.igapyon.mikutextbundle.model.CollectedFile;
import jp.igapyon.mikutextbundle.model.Marker;
import jp.igapyon.mikutextbundle.model.SkippedFile;

public final class Markdown {
    private static final Map<String, String> EXTENSION_LANGUAGES = new HashMap<String, String>();

    static {
        EXTENSION_LANGUAGES.put("ts", "ts");
        EXTENSION_LANGUAGES.put("tsx", "tsx");
        EXTENSION_LANGUAGES.put("js", "js");
        EXTENSION_LANGUAGES.put("jsx", "jsx");
        EXTENSION_LANGUAGES.put("mjs", "js");
        EXTENSION_LANGUAGES.put("cjs", "js");
        EXTENSION_LANGUAGES.put("java", "java");
        EXTENSION_LANGUAGES.put("cs", "csharp");
        EXTENSION_LANGUAGES.put("md", "md");
        EXTENSION_LANGUAGES.put("json", "json");
    }

    private Markdown() {
    }

    public static String buildPartMarkdown(BundlePart part) {
        List<String> lines = new ArrayList<String>();
        lines.add("# Text Bundle Part " + pad3(part.partNumber));
        lines.add("");
        lines.add("- Part file: `" + part.fileName + "`");
        lines.add("- Files/chunks: " + part.chunks.size());
        lines.add("- Approx chars: " + part.charCount);
        lines.add("");

        for (BundleChunk chunk : part.chunks) {
            lines.addAll(buildChunkMarkdown(chunk));
        }

        return markdown(lines);
    }

    public static String buildIndexMarkdown(String inputDirectory, String outputDirectory, List<BundlePart> parts,
            List<CollectedFile> collectedFiles, List<SkippedFile> skippedFiles, List<Marker> markers, List<String> warnings) {
        List<String> lines = new ArrayList<String>();
        lines.add("# Text Bundle Index");
        lines.add("");
        lines.add("## Summary");
        lines.add("");
        lines.add("- Input directory: `" + inputDirectory + "`");
        lines.add("- Output directory: `" + outputDirectory + "`");
        lines.add("- Collected files: " + collectedFiles.size());
        lines.add("- Skipped files: " + skippedFiles.size());
        lines.add("- Parts: " + parts.size());
        lines.add("");
        lines.add("## Parts");
        lines.add("");
        lines.addAll(partsTable(parts));
        lines.add("## Skipped Files");
        lines.add("");
        lines.addAll(skippedFilesTable(skippedFiles));
        lines.add("## Warnings");
        lines.add("");
        lines.addAll(warningList(warnings));
        lines.add("## Markers");
        lines.add("");
        lines.add(markerTable(markers));
        return markdown(lines);
    }

    public static String buildPromptMarkdown(List<String> partFileNames) {
        List<String> lines = new ArrayList<String>();
        lines.add("# Text Bundle Prompt");
        lines.add("");
        lines.add("これから Markdown バンドルを複数のメッセージに分けて順番に送ります。");
        lines.add("");
        lines.add("各メッセージを受け取ったら、内容の分析や要約はまだ行わず、`受領しました` とだけ返してください。");
        lines.add("");
        lines.add("`END_OF_TEXT_BUNDLE` という完了合図を受け取るまで、最終回答を開始しないでください。");
        lines.add("");
        lines.add("## 読み込み順");
        lines.add("");
        lines.add("1. `text-bundle-000-index.md`");
        for (int i = 0; i < partFileNames.size(); i++) {
            lines.add((i + 2) + ". `" + partFileNames.get(i) + "`");
        }
        lines.add((partFileNames.size() + 2) + ". `END_OF_TEXT_BUNDLE`");
        lines.add("");
        lines.add("## 回答ファイル");
        lines.add("");
        lines.add("`END_OF_TEXT_BUNDLE` の後に作成する回答は `text-bundle-response.md` として保存する想定です。");
        lines.add("");
        lines.add("## 出力形式");
        lines.add("");
        lines.add("markdown テキスト形式で出力してください。");
        lines.add("");
        lines.add("○最終的な回答は Markdown テキスト形式で出力し、さらに ~~~~ で囲まれた一塊として出力してください。markdown 内に backtick による code fence が含まれる場合があるため、外側の囲みは tilde を使ってください。");
        lines.add("");
        return join(lines, "\n");
    }

    private static List<String> buildChunkMarkdown(BundleChunk chunk) {
        List<String> lines = new ArrayList<String>();
        lines.add("### " + chunk.relativePath);
        lines.add("");
        lines.add("- Characters: " + chunk.content.length());
        lines.add("- Source characters: " + chunk.originalCharCount);
        lines.add("- Source lines: " + chunk.originalLineCount);

        if (chunk.splitReason != null) {
            lines.add("- Warning: " + chunk.splitReason);
            lines.add("- Split: " + chunk.chunkIndex + " / " + chunk.chunkCount);
        }

        lines.add("");
        if (chunk.splitReason != null) {
            lines.add("このファイルはサイズ上限を超えたため、やむを得ず分割しました。元ファイル: `" + chunk.relativePath + "`。分割: " + chunk.chunkIndex + " / " + chunk.chunkCount + "。");
            lines.add("");
        }

        String fence = fenceFor(chunk.content);
        lines.add(fence + languageFor(chunk.extension));
        lines.add(chunk.content);
        lines.add(fence);
        lines.add("");
        return lines;
    }

    private static String fenceFor(String content) {
        int longest = 2;
        int current = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '`') {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return repeat("`", longest + 1);
    }

    private static String languageFor(String extension) {
        String language = EXTENSION_LANGUAGES.get(extension);
        return language == null ? "" : language;
    }

    private static List<String> partsTable(List<BundlePart> parts) {
        List<List<String>> rows = new ArrayList<List<String>>();
        for (BundlePart part : parts) {
            List<String> files = new ArrayList<String>();
            for (BundleChunk chunk : part.chunks) {
                files.add(code(chunk.relativePath));
            }
            List<String> row = new ArrayList<String>();
            row.add(code(part.fileName));
            row.add(String.valueOf(part.chunks.size()));
            row.add(String.valueOf(part.charCount));
            row.add(join(files, "<br>"));
            rows.add(row);
        }
        return table(new String[] { "Part", "Chunks", "Approx chars", "Files" },
                new String[] { "---", "---:", "---:", "---" }, rows);
    }

    private static List<String> skippedFilesTable(List<SkippedFile> skippedFiles) {
        if (skippedFiles.isEmpty()) {
            List<String> lines = new ArrayList<String>();
            lines.add("- なし");
            lines.add("");
            return lines;
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        for (SkippedFile file : skippedFiles) {
            List<String> row = new ArrayList<String>();
            row.add(code(file.relativePath));
            row.add(escapeTable(file.reason));
            rows.add(row);
        }
        return table(new String[] { "File", "Reason" }, new String[] { "---", "---" }, rows);
    }

    private static String markerTable(List<Marker> markers) {
        if (markers.isEmpty()) {
            return "- なし\n";
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        for (Marker marker : markers) {
            List<String> row = new ArrayList<String>();
            row.add(code(marker.relativePath));
            row.add(String.valueOf(marker.line));
            row.add(marker.kind);
            row.add(escapeTable(marker.text));
            rows.add(row);
        }
        return join(table(new String[] { "File", "Line", "Kind", "Text" },
                new String[] { "---", "---:", "---", "---" }, rows), "\n");
    }

    private static List<String> warningList(List<String> warnings) {
        List<String> lines = new ArrayList<String>();
        if (warnings.isEmpty()) {
            lines.add("- なし");
        } else {
            for (String warning : warnings) {
                lines.add("- " + warning);
            }
        }
        lines.add("");
        return lines;
    }

    private static List<String> table(String[] headers, String[] alignments, List<List<String>> rows) {
        List<String> lines = new ArrayList<String>();
        lines.add("| " + join(headers, " | ") + " |");
        lines.add("| " + join(alignments, " | ") + " |");
        for (List<String> row : rows) {
            lines.add("| " + join(row, " | ") + " |");
        }
        lines.add("");
        return lines;
    }

    private static String markdown(List<String> lines) {
        return join(lines, "\n").replaceAll("\\n{3,}", "\n\n") + "\n";
    }

    private static String code(String value) {
        return "`" + value + "`";
    }

    private static String escapeTable(String value) {
        return value.replace("|", "\\|").replaceAll("\\r?\\n", " ");
    }

    private static String pad3(int value) {
        return String.format("%03d", value);
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private static String join(String[] values, String separator) {
        List<String> list = new ArrayList<String>();
        for (String value : values) {
            list.add(value);
        }
        return join(list, separator);
    }

    private static String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }
}
