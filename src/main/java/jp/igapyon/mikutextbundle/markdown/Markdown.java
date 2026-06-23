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
import jp.igapyon.mikutextbundle.core.MikuTextBundle;

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
        return buildPartMarkdown(part, null, null, false);
    }

    public static String buildPartMarkdown(BundlePart part, PromptOptions prompt, IndexOptions index) {
        return buildPartMarkdown(part, prompt, index, false);
    }

    public static String buildPartMarkdown(BundlePart part, PromptOptions prompt, IndexOptions index, boolean acknowledgeOnly) {
        List<String> lines = new ArrayList<String>();
        List<String> extraFrontMatter = new ArrayList<String>();
        extraFrontMatter.add("part: " + part.partNumber);
        if (prompt != null) {
            extraFrontMatter.add("prompt: true");
        }
        if (index != null) {
            extraFrontMatter.add("terminal: true");
        }
        lines.addAll(frontMatter("part", extraFrontMatter));
        if (prompt != null) {
            lines.addAll(buildPromptMarkdownLines(prompt, false));
        }
        lines.add("# Text Bundle Part " + pad3(part.partNumber));
        lines.add("");
        lines.add("- Part file: `" + part.fileName + "`");
        lines.add("- Files/chunks: " + part.chunks.size());
        lines.add("- Approx chars: " + part.charCount);
        lines.add("");

        for (int i = 0; i < part.chunks.size(); i++) {
            if (i > 0) {
                lines.add("---");
                lines.add("");
            }
            BundleChunk chunk = part.chunks.get(i);
            lines.addAll(buildChunkMarkdown(chunk));
        }

        if (index != null) {
            lines.addAll(buildIndexMarkdownLines(index, false));
        }

        if (acknowledgeOnly) {
            lines.addAll(buildAcknowledgementFooterLines());
        }

        return markdown(lines);
    }

    public static String buildIndexMarkdown(String inputDirectory, String outputDirectory, List<BundlePart> parts,
            List<CollectedFile> collectedFiles, List<SkippedFile> skippedFiles, List<Marker> markers, List<String> warnings) {
        String terminalFileName = parts.isEmpty() ? "the final part file" : parts.get(parts.size() - 1).fileName;
        return buildIndexMarkdown(new IndexOptions(inputDirectory, outputDirectory, parts, collectedFiles, skippedFiles,
                markers, warnings, terminalFileName));
    }

    public static String buildIndexMarkdown(IndexOptions options) {
        return markdown(buildIndexMarkdownLines(options, true));
    }

    private static List<String> buildIndexMarkdownLines(IndexOptions options, boolean includeFrontMatter) {
        List<String> lines = new ArrayList<String>();
        if (includeFrontMatter) {
            lines.addAll(frontMatter("index", "terminal: true"));
        }
        lines.add("# Text Bundle Index");
        lines.add("");
        lines.add("## Summary");
        lines.add("");
        lines.add("- Input directory: `" + options.inputDirectory + "`");
        lines.add("- Output directory: `" + options.outputDirectory + "`");
        lines.add("- Collected files: " + options.collectedFiles.size());
        lines.add("- Skipped files: " + options.skippedFiles.size());
        lines.add("- Parts: " + options.parts.size());
        lines.add("");
        lines.addAll(agentSkillHandoffSection(agentSkillPaths(options.collectedFiles), options.terminalFileName));
        lines.add("## Parts");
        lines.add("");
        lines.addAll(partsTable(options.parts));
        lines.add("## Skipped Files");
        lines.add("");
        lines.addAll(skippedFilesTable(options.skippedFiles));
        lines.add("## Warnings");
        lines.add("");
        lines.addAll(warningList(options.warnings));
        lines.add("## Markers");
        lines.add("");
        lines.add(markerTable(options.markers));
        return lines;
    }

    public static String buildPromptMarkdown(List<String> partFileNames) {
        String promptFileName = partFileNames.isEmpty() ? "text-bundle-001.md" : partFileNames.get(0);
        String indexFileName = partFileNames.isEmpty() ? promptFileName : partFileNames.get(partFileNames.size() - 1);
        return buildPromptMarkdown(promptFileName, partFileNames, indexFileName);
    }

    public static String buildPromptMarkdown(String promptFileName, List<String> partFileNames, String indexFileName) {
        return join(buildPromptMarkdownLines(new PromptOptions(promptFileName, partFileNames, indexFileName), true), "\n");
    }

    private static List<String> buildPromptMarkdownLines(PromptOptions options, boolean includeFrontMatter) {
        List<String> lines = new ArrayList<String>();
        if (includeFrontMatter) {
            lines.addAll(frontMatter("prompt"));
        }
        lines.add("# Text Bundle Prompt");
        lines.add("");
        lines.add("This is the reading instruction for a Text Bundle that packages a set of files for handoff to generative AI or similar tools.");
        lines.add("");
        lines.add("The Markdown bundle will be sent in multiple messages in the order listed below.");
        lines.add("");
        lines.add("After each non-terminal Part, do not analyze or summarize the content yet. Reply only with `OK`.");
        lines.add("");
        lines.add("Do not start the final response until you receive `" + options.indexFileName + "`.");
        lines.add("");
        lines.add("## Reading Order");
        lines.add("");
        List<String> readingOrderFileNames = readingOrderFileNames(options);
        for (int i = 0; i < readingOrderFileNames.size(); i++) {
            lines.add((i + 1) + ". `" + readingOrderFileNames.get(i) + "`");
        }
        lines.add("");
        lines.add("## Response File");
        lines.add("");
        lines.add("If you save the final response after `" + options.indexFileName + "`, `text-bundle-response.md` is the recommended filename.");
        lines.add("");
        lines.add("## Output Format");
        lines.add("");
        lines.add("Output the final response as Markdown text.");
        lines.add("");
        lines.add("Wrap the entire final Markdown response in a single outer fence using `~~~~`. Use tildes for the outer fence because the Markdown response may contain backtick code fences.");
        lines.add("");
        return lines;
    }

    private static List<String> readingOrderFileNames(PromptOptions options) {
        List<String> names = new ArrayList<String>();
        if (options.partFileNames.isEmpty() || !options.partFileNames.get(0).equals(options.promptFileName)) {
            names.add(options.promptFileName);
        }
        names.addAll(options.partFileNames);
        if (options.partFileNames.isEmpty() || !options.partFileNames.get(options.partFileNames.size() - 1).equals(options.indexFileName)) {
            names.add(options.indexFileName);
        }
        return names;
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
            lines.add("This file exceeded the size limit and was split. Source file: `" + chunk.relativePath + "`. Split: " + chunk.chunkIndex + " / " + chunk.chunkCount + ".");
            lines.add("");
        }

        String fence = fenceFor(chunk.content);
        lines.add(fence + languageFor(chunk.extension));
        lines.add(chunk.content);
        lines.add(fence);
        lines.add("");
        return lines;
    }

    private static List<String> buildAcknowledgementFooterLines() {
        List<String> lines = new ArrayList<String>();
        lines.add("## Acknowledgement");
        lines.add("");
        lines.add("After reading this Part, do not analyze or summarize the content yet. Reply only with `OK`.");
        lines.add("");
        return lines;
    }

    private static String fenceFor(String content) {
        int longest = 2;
        int current = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '~') {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return repeat("~", longest + 1);
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
            lines.add("- None");
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
            return "- None\n";
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
            lines.add("- None");
        } else {
            for (String warning : warnings) {
                lines.add("- " + warning);
            }
        }
        lines.add("");
        return lines;
    }

    private static List<String> agentSkillPaths(List<CollectedFile> collectedFiles) {
        List<String> skillPaths = new ArrayList<String>();
        for (CollectedFile file : collectedFiles) {
            if (isAgentSkillPath(file.relativePath)) {
                skillPaths.add(file.relativePath);
            }
        }
        return skillPaths;
    }

    private static boolean isAgentSkillPath(String relativePath) {
        if ("SKILL.md".equals(relativePath)) {
            return true;
        }
        return relativePath.matches("skills/[^/]+/SKILL\\.md");
    }

    private static List<String> agentSkillHandoffSection(List<String> skillPaths, String terminalFileName) {
        List<String> lines = new ArrayList<String>();
        if (skillPaths.isEmpty()) {
            return lines;
        }

        lines.add("## Agent Skill Handoff");
        lines.add("");
        lines.add("This Text Bundle includes Agent Skill definitions.");
        lines.add("");
        lines.add("Read the following `SKILL.md` files as the primary Agent Skill instructions and keep them available for reference in this conversation.");
        lines.add("");
        for (String skillPath : skillPaths) {
            lines.add("- " + code(skillPath));
        }
        lines.add("");
        lines.add("After receiving `" + terminalFileName + "`, the Text Bundle loading is complete. If the user asks to use this Agent Skill or the target product, prioritize the relevant `SKILL.md` activation rules, operating rules, workflow, and references, and work from the related files in this bundle.");
        lines.add("");
        lines.add("If higher-priority system, developer, or user instructions apply, follow those instructions first.");
        lines.add("");
        return lines;
    }

    private static List<String> frontMatter(String role, String extra) {
        List<String> extras = new ArrayList<String>();
        extras.add(extra);
        return frontMatter(role, extras);
    }

    private static List<String> frontMatter(String role, List<String> extras) {
        List<String> lines = frontMatter(role);
        lines.addAll(lines.size() - 2, extras);
        return lines;
    }

    private static List<String> frontMatter(String role) {
        List<String> lines = new ArrayList<String>();
        lines.add("---");
        lines.add("tool: miku-text-bundle");
        lines.add("version: " + MikuTextBundle.VERSION);
        lines.add("role: " + role);
        lines.add("---");
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

    public static final class PromptOptions {
        public final String promptFileName;
        public final List<String> partFileNames;
        public final String indexFileName;

        public PromptOptions(String promptFileName, List<String> partFileNames, String indexFileName) {
            this.promptFileName = promptFileName;
            this.partFileNames = new ArrayList<String>(partFileNames);
            this.indexFileName = indexFileName;
        }
    }

    public static final class IndexOptions {
        public final String inputDirectory;
        public final String outputDirectory;
        public final List<BundlePart> parts;
        public final List<CollectedFile> collectedFiles;
        public final List<SkippedFile> skippedFiles;
        public final List<Marker> markers;
        public final List<String> warnings;
        public final String terminalFileName;

        public IndexOptions(String inputDirectory, String outputDirectory, List<BundlePart> parts,
                List<CollectedFile> collectedFiles, List<SkippedFile> skippedFiles, List<Marker> markers,
                List<String> warnings, String terminalFileName) {
            this.inputDirectory = inputDirectory;
            this.outputDirectory = outputDirectory;
            this.parts = parts;
            this.collectedFiles = collectedFiles;
            this.skippedFiles = skippedFiles;
            this.markers = markers;
            this.warnings = warnings;
            this.terminalFileName = terminalFileName;
        }
    }
}
