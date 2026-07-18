package jp.igapyon.mikutextbundle.markdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.igapyon.mikutextbundle.model.BundleChunk;
import jp.igapyon.mikutextbundle.model.BundlePart;
import jp.igapyon.mikutextbundle.model.CollectedFile;
import jp.igapyon.mikutextbundle.model.Marker;
import jp.igapyon.mikutextbundle.model.SkippedFile;
import jp.igapyon.mikutextbundle.core.MikuTextBundle;

public final class Markdown {
    private static final Map<String, LanguageDetails> EXTENSION_LANGUAGES = new HashMap<String, LanguageDetails>();
    private static final Map<String, LanguageDetails> FILE_NAME_LANGUAGES = new HashMap<String, LanguageDetails>();
    private static final LanguageDetails UNKNOWN_LANGUAGE = new LanguageDetails("Unknown", "", "Source content block");

    static {
        EXTENSION_LANGUAGES.put("ts", sourceCode("TypeScript", "ts"));
        EXTENSION_LANGUAGES.put("mts", sourceCode("TypeScript", "ts"));
        EXTENSION_LANGUAGES.put("cts", sourceCode("TypeScript", "ts"));
        EXTENSION_LANGUAGES.put("tsx", sourceCode("TypeScript TSX", "tsx"));
        EXTENSION_LANGUAGES.put("js", sourceCode("JavaScript", "js"));
        EXTENSION_LANGUAGES.put("jsx", sourceCode("JavaScript JSX", "jsx"));
        EXTENSION_LANGUAGES.put("mjs", sourceCode("JavaScript", "js"));
        EXTENSION_LANGUAGES.put("cjs", sourceCode("JavaScript", "js"));
        EXTENSION_LANGUAGES.put("java", sourceCode("Java", "java"));
        EXTENSION_LANGUAGES.put("cs", sourceCode("C#", "csharp"));
        EXTENSION_LANGUAGES.put("py", sourceCode("Python", "python"));
        EXTENSION_LANGUAGES.put("pyw", sourceCode("Python", "python"));
        EXTENSION_LANGUAGES.put("go", sourceCode("Go", "go"));
        EXTENSION_LANGUAGES.put("rs", sourceCode("Rust", "rust"));
        EXTENSION_LANGUAGES.put("c", sourceCode("C", "c"));
        EXTENSION_LANGUAGES.put("h", sourceCode("C header", "c"));
        EXTENSION_LANGUAGES.put("cc", sourceCode("C++", "cpp"));
        EXTENSION_LANGUAGES.put("cpp", sourceCode("C++", "cpp"));
        EXTENSION_LANGUAGES.put("cxx", sourceCode("C++", "cpp"));
        EXTENSION_LANGUAGES.put("hh", sourceCode("C++ header", "cpp"));
        EXTENSION_LANGUAGES.put("hpp", sourceCode("C++ header", "cpp"));
        EXTENSION_LANGUAGES.put("hxx", sourceCode("C++ header", "cpp"));
        EXTENSION_LANGUAGES.put("swift", sourceCode("Swift", "swift"));
        EXTENSION_LANGUAGES.put("kt", sourceCode("Kotlin", "kotlin"));
        EXTENSION_LANGUAGES.put("kts", sourceCode("Kotlin Script", "kotlin"));
        EXTENSION_LANGUAGES.put("scala", sourceCode("Scala", "scala"));
        EXTENSION_LANGUAGES.put("rb", sourceCode("Ruby", "ruby"));
        EXTENSION_LANGUAGES.put("php", sourceCode("PHP", "php"));
        EXTENSION_LANGUAGES.put("sh", sourceCode("Shell", "bash"));
        EXTENSION_LANGUAGES.put("bash", sourceCode("Bash", "bash"));
        EXTENSION_LANGUAGES.put("zsh", sourceCode("Z shell", "zsh"));
        EXTENSION_LANGUAGES.put("fish", sourceCode("fish shell", "fish"));
        EXTENSION_LANGUAGES.put("ps1", sourceCode("PowerShell", "powershell"));
        EXTENSION_LANGUAGES.put("sql", sourceCode("SQL", "sql"));
        EXTENSION_LANGUAGES.put("html", sourceCode("HTML", "html"));
        EXTENSION_LANGUAGES.put("htm", sourceCode("HTML", "html"));
        EXTENSION_LANGUAGES.put("css", sourceCode("CSS", "css"));
        EXTENSION_LANGUAGES.put("scss", sourceCode("SCSS", "scss"));
        EXTENSION_LANGUAGES.put("sass", sourceCode("Sass", "sass"));
        EXTENSION_LANGUAGES.put("less", sourceCode("Less", "less"));
        EXTENSION_LANGUAGES.put("vue", sourceCode("Vue", "vue"));
        EXTENSION_LANGUAGES.put("svelte", sourceCode("Svelte", "svelte"));
        EXTENSION_LANGUAGES.put("groovy", sourceCode("Groovy", "groovy"));
        EXTENSION_LANGUAGES.put("gradle", sourceCode("Gradle", "groovy"));
        EXTENSION_LANGUAGES.put("md", sourceText("Markdown", "md"));
        EXTENSION_LANGUAGES.put("markdown", sourceText("Markdown", "md"));
        EXTENSION_LANGUAGES.put("txt", sourceText("Plain text", "text"));
        EXTENSION_LANGUAGES.put("rst", sourceText("reStructuredText", "rst"));
        EXTENSION_LANGUAGES.put("adoc", sourceText("AsciiDoc", "asciidoc"));
        EXTENSION_LANGUAGES.put("json", sourceCode("JSON", "json"));
        EXTENSION_LANGUAGES.put("jsonl", sourceCode("JSON Lines", "json"));
        EXTENSION_LANGUAGES.put("yaml", sourceText("YAML", "yaml"));
        EXTENSION_LANGUAGES.put("yml", sourceText("YAML", "yaml"));
        EXTENSION_LANGUAGES.put("xml", sourceText("XML", "xml"));
        EXTENSION_LANGUAGES.put("toml", sourceText("TOML", "toml"));
        EXTENSION_LANGUAGES.put("ini", sourceText("INI", "ini"));
        EXTENSION_LANGUAGES.put("cfg", sourceText("Configuration", "ini"));
        EXTENSION_LANGUAGES.put("conf", sourceText("Configuration", "text"));
        EXTENSION_LANGUAGES.put("properties", sourceText("Java properties", "properties"));
        EXTENSION_LANGUAGES.put("csv", sourceText("CSV", "csv"));
        EXTENSION_LANGUAGES.put("tsv", sourceText("TSV", "tsv"));

        FILE_NAME_LANGUAGES.put("dockerfile", sourceCode("Dockerfile", "dockerfile"));
        FILE_NAME_LANGUAGES.put("containerfile", sourceCode("Containerfile", "dockerfile"));
        FILE_NAME_LANGUAGES.put("makefile", sourceCode("Makefile", "makefile"));
        FILE_NAME_LANGUAGES.put("gnumakefile", sourceCode("GNU Makefile", "makefile"));
        FILE_NAME_LANGUAGES.put("gradlew", sourceCode("Shell", "bash"));
        FILE_NAME_LANGUAGES.put(".gitignore", sourceText("Git ignore rules", "gitignore"));
        FILE_NAME_LANGUAGES.put(".gitattributes", sourceText("Git attributes", "gitattributes"));
        FILE_NAME_LANGUAGES.put(".editorconfig", sourceText("EditorConfig", "editorconfig"));
        FILE_NAME_LANGUAGES.put(".npmrc", sourceText("npm configuration", "ini"));
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

        for (BundleChunk chunk : part.chunks) {
            lines.addAll(buildFileBlockMarkdown(chunk));
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

    private static List<String> buildFileBlockMarkdown(BundleChunk chunk) {
        List<String> lines = new ArrayList<String>();
        String path = displayPath(chunk.relativePath);
        lines.add("### FILE: " + path);
        lines.add("");
        lines.add("--- BEGIN FILE: " + path + " ---");
        lines.add("");
        if (chunk.chunkCount > 1) {
            lines.add("Chunk: " + chunk.chunkIndex + " / " + chunk.chunkCount);
            lines.add("Source lines: " + chunk.sourceStartLine + "-" + chunk.sourceEndLine);
            lines.add("");
        }
        LanguageDetails language = languageFor(chunk.extension, chunk.relativePath);
        String fence = fenceFor(chunk.content);
        String fencedContent = fence + language.fenceLanguage + "\n" + chunk.content
                + (chunk.content.endsWith("\n") ? "" : "\n") + fence;
        lines.add(language.blockLabel);
        lines.add("Language: " + language.displayName);
        lines.add("");
        lines.add(fencedContent);
        lines.add("");
        lines.add("--- END FILE: " + path + " ---");
        lines.add("");
        return lines;
    }

    public static String buildKnowledgeSourceMarkdown(BundlePart part) {
        List<String> lines = new ArrayList<String>();
        lines.add("# Knowledge Source " + pad3(part.partNumber));
        lines.add("");
        for (BundleChunk chunk : part.chunks) {
            lines.addAll(buildFileBlockMarkdown(chunk));
        }
        return join(lines, "\n") + "\n";
    }

    public static String buildKnowledgeIndexMarkdown(KnowledgeIndexOptions options) {
        List<String> lines = new ArrayList<String>();
        lines.add("# Knowledge Bundle Index");
        lines.add("");
        lines.add("## Configuration");
        lines.add("");
        List<List<String>> configurationRows = new ArrayList<List<String>>();
        for (String[] entry : options.configuration) {
            List<String> row = new ArrayList<String>();
            row.add(code(entry[0]));
            row.add(escapeTable(entry[1]));
            configurationRows.add(row);
        }
        lines.addAll(table(new String[] { "Option", "Effective value" }, new String[] { "---", "---" }, configurationRows));
        lines.add("## Summary");
        lines.add("");
        lines.add("- Collected files: " + options.collectedFiles.size());
        lines.add("- Skipped files: " + options.skippedFiles.size());
        lines.add("- Knowledge files: " + options.parts.size());
        lines.add("");
        lines.add("## Generated Files");
        lines.add("");
        List<List<String>> generatedRows = new ArrayList<List<String>>();
        for (BundlePart part : options.parts) {
            generatedRows.add(row(code(part.fileName), "knowledge-source", String.valueOf(part.chunks.size()), String.valueOf(part.charCount)));
        }
        generatedRows.add(row(code(options.managementIndexFileName), "management-index", "-", "-"));
        lines.addAll(table(new String[] { "File", "Role", "Chunks", "Approx chars" }, new String[] { "---", "---", "---:", "---:" }, generatedRows));
        lines.add("## Source Mapping");
        lines.add("");
        List<List<String>> mappingRows = new ArrayList<List<String>>();
        for (BundlePart part : options.parts) {
            for (BundleChunk chunk : part.chunks) {
                mappingRows.add(row(code(chunk.relativePath), code(part.fileName), chunk.chunkIndex + " / " + chunk.chunkCount,
                        chunk.sourceStartLine + "-" + chunk.sourceEndLine, chunk.sourceStartChar + "-" + chunk.sourceEndChar,
                        String.valueOf(chunk.originalCharCount), String.valueOf(chunk.content.length())));
            }
        }
        lines.addAll(table(new String[] { "Source", "Generated file", "Chunk", "Source lines", "UTF-16 chars", "Source chars", "Chunk chars" },
                new String[] { "---", "---", "---:", "---:", "---:", "---:", "---:" }, mappingRows));
        lines.add("## Skipped Files"); lines.add(""); lines.addAll(skippedFilesTable(options.skippedFiles));
        lines.add("## Warnings"); lines.add(""); lines.addAll(warningList(options.warnings));
        lines.add("## Markers"); lines.add(""); lines.add(markerTable(options.markers));
        lines.add("## Stale Output Candidates"); lines.add("");
        if (options.staleOutputCandidates.isEmpty()) { lines.add("- None"); lines.add(""); }
        else { for (String value : options.staleOutputCandidates) lines.add("- " + code(value)); lines.add(""); }
        return markdown(lines);
    }

    private static List<String> row(String... values) {
        List<String> row = new ArrayList<String>();
        for (String value : values) row.add(value);
        return row;
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

    private static LanguageDetails languageFor(String extension, String relativePath) {
        LanguageDetails language = EXTENSION_LANGUAGES.get(extension);
        if (language != null) {
            return language;
        }
        int slash = relativePath.lastIndexOf('/');
        String fileName = relativePath.substring(slash + 1).toLowerCase(Locale.ROOT);
        language = FILE_NAME_LANGUAGES.get(fileName);
        return language == null ? UNKNOWN_LANGUAGE : language;
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
        List<String> compacted = new ArrayList<String>();
        for (String line : lines) {
            if (!line.isEmpty() || compacted.isEmpty() || !compacted.get(compacted.size() - 1).isEmpty()) {
                compacted.add(line);
            }
        }
        return join(compacted, "\n") + "\n";
    }

    private static String code(String value) {
        String displayed = displayPath(value).replace("|", "\\|");
        int longest = 0;
        int current = 0;
        for (int i = 0; i < displayed.length(); i++) {
            if (displayed.charAt(i) == '`') {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        String delimiter = repeat("`", longest + 1);
        return delimiter + displayed + delimiter;
    }

    private static String displayPath(String relativePath) {
        StringBuilder displayed = new StringBuilder();
        for (int i = 0; i < relativePath.length(); i++) {
            char character = relativePath.charAt(i);
            if (character == '\\') {
                displayed.append("\\\\");
            } else if (character == '\n') {
                displayed.append("\\n");
            } else if (character == '\r') {
                displayed.append("\\r");
            } else if (character == '\t') {
                displayed.append("\\t");
            } else if (character <= 0x1f || character == 0x7f) {
                displayed.append(String.format("\\u%04x", Integer.valueOf(character)));
            } else {
                displayed.append(character);
            }
        }
        return displayed.toString();
    }

    private static LanguageDetails sourceCode(String displayName, String fenceLanguage) {
        return new LanguageDetails(displayName, fenceLanguage, "Source code block");
    }

    private static LanguageDetails sourceText(String displayName, String fenceLanguage) {
        return new LanguageDetails(displayName, fenceLanguage, "Source text block");
    }

    private static final class LanguageDetails {
        private final String displayName;
        private final String fenceLanguage;
        private final String blockLabel;

        private LanguageDetails(String displayName, String fenceLanguage, String blockLabel) {
            this.displayName = displayName;
            this.fenceLanguage = fenceLanguage;
            this.blockLabel = blockLabel;
        }
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

    public static final class KnowledgeIndexOptions {
        public String managementIndexFileName;
        public List<String[]> configuration = new ArrayList<String[]>();
        public List<BundlePart> parts = new ArrayList<BundlePart>();
        public List<CollectedFile> collectedFiles = new ArrayList<CollectedFile>();
        public List<SkippedFile> skippedFiles = new ArrayList<SkippedFile>();
        public List<Marker> markers = new ArrayList<Marker>();
        public List<String> warnings = new ArrayList<String>();
        public List<String> staleOutputCandidates = new ArrayList<String>();
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
