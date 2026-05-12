package jp.igapyon.mikutextbundle.coreapi;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.igapyon.mikutextbundle.discovery.DiscoveryResult;
import jp.igapyon.mikutextbundle.discovery.FileDiscovery;
import jp.igapyon.mikutextbundle.markdown.Markdown;
import jp.igapyon.mikutextbundle.match.PatternMatcher;
import jp.igapyon.mikutextbundle.model.BundleChunk;
import jp.igapyon.mikutextbundle.model.BundlePart;
import jp.igapyon.mikutextbundle.model.CliOptions;
import jp.igapyon.mikutextbundle.model.CollectedFile;
import jp.igapyon.mikutextbundle.model.IgnoreStats;
import jp.igapyon.mikutextbundle.model.Marker;
import jp.igapyon.mikutextbundle.model.SkippedFile;
import jp.igapyon.mikutextbundle.model.SupportedEncoding;
import jp.igapyon.mikutextbundle.pathutils.PathUtils;

public class TextBundler {
    private static final String INDEX_FILE_NAME = "text-bundle-000-index.md";
    private static final String PROMPT_FILE_NAME = "text-bundle-000-prompt.md";
    private static final Pattern MARKER_PATTERN = Pattern.compile("\\b(TODO|FIXME|XXX)\\b(?!\\.)(.*)");

    public BundleResult createTextBundle(CliOptions options) throws IOException {
        return createTextBundle(options, new Date(), System.out);
    }

    public BundleResult createTextBundle(CliOptions options, Date now, PrintStream out) throws IOException {
        Path inputPath = Paths.get(options.inputDirectory).toAbsolutePath().normalize();
        if (!Files.isDirectory(inputPath)) {
            throw new IllegalArgumentException("Input directory does not exist: " + inputPath);
        }

        Path outputDirectory = chooseOutputDirectory(options.outputDirectory);
        Files.createDirectories(outputDirectory);

        List<String> gitignorePatterns = readRootGitignore(inputPath);
        CollectedFilesResult collected = collectFiles(inputPath, outputDirectory, options, gitignorePatterns);
        List<Marker> markers = collectMarkers(collected.files);
        BundlePartsResult partsResult = buildParts(collected.files, options.maxChars);

        BundleMarkdownPaths paths = writeBundleMarkdownFiles(outputDirectory, inputPath, partsResult.parts, collected.files,
                collected.skipped, markers, partsResult.warnings);

        if (options.verbose) {
            out.println("collected=" + collected.files.size());
            out.println("skipped=" + collected.skipped.size());
            out.println("parts=" + partsResult.parts.size());
            out.println("ignoredDirectories=" + collected.ignored.directories);
            out.println("ignoredFiles=" + collected.ignored.files);
            out.println("ignoredByDirectory=" + collected.ignored.byDirectory);
            out.println("ignoredByExtension=" + collected.ignored.byExtension);
            out.println("ignoredByGitignore=" + collected.ignored.byGitignore);
            out.println("ignoredByOutputDirectory=" + collected.ignored.byOutputDirectory);
        }

        out.println("generated: " + paths.indexPath);
        for (String partPath : paths.partPaths) {
            out.println("generated: " + partPath);
        }
        out.println("generated: " + paths.promptPath);

        BundleResult result = new BundleResult();
        result.outputDirectory = outputDirectory.toString();
        result.indexPath = paths.indexPath;
        result.promptPath = paths.promptPath;
        result.partPaths.addAll(paths.partPaths);
        result.filesCollected = collected.files.size();
        result.filesSkipped = collected.skipped.size();
        result.directoriesIgnored = collected.ignored.directories;
        result.filesIgnored = collected.ignored.files;
        result.ignoredByDirectory = collected.ignored.byDirectory;
        result.ignoredByExtension = collected.ignored.byExtension;
        result.ignoredByGitignore = collected.ignored.byGitignore;
        result.ignoredByOutputDirectory = collected.ignored.byOutputDirectory;
        result.partsGenerated = partsResult.parts.size();
        result.warnings.addAll(partsResult.warnings);
        return result;
    }

    public static Path chooseOutputDirectory(String outputDirectory) {
        return Paths.get(outputDirectory).toAbsolutePath().normalize();
    }

    private List<String> readRootGitignore(Path inputPath) throws IOException {
        Path gitignorePath = inputPath.resolve(".gitignore");
        if (!Files.isRegularFile(gitignorePath)) {
            return new ArrayList<String>();
        }
        return PatternMatcher.parseGitignore(new String(Files.readAllBytes(gitignorePath), StandardCharsets.UTF_8));
    }

    private CollectedFilesResult collectFiles(Path inputPath, Path outputPath, CliOptions options,
            List<String> gitignorePatterns)
            throws IOException {
        List<CollectedFile> files = new ArrayList<CollectedFile>();
        List<SkippedFile> skipped = new ArrayList<SkippedFile>();
        int maxInputFileBytes = options.maxInputFileBytes;
        DiscoveryResult discovered = FileDiscovery.discoverCandidateFiles(inputPath, outputPath, options, gitignorePatterns);

        for (Path filePath : discovered.files) {
            String relativePath = relativeInputPath(inputPath, filePath);
            long size = Files.size(filePath);
            if (size > maxInputFileBytes) {
                skipped.add(skippedForOversizedFile(relativePath, maxInputFileBytes));
                continue;
            }

            byte[] bytes = Files.readAllBytes(filePath);
            SupportedEncoding encoding = selectEncoding(relativePath, options);
            String content = decodeText(bytes, encoding);
            if (content == null) {
                skipped.add(skippedForUnreadableFile(relativePath, encoding));
                continue;
            }

            files.add(createCollectedFile(filePath, relativePath, content));
        }

        return new CollectedFilesResult(files, skipped, discovered.ignored);
    }

    private SupportedEncoding selectEncoding(String relativePath, CliOptions options) {
        if (options.encoding == null) {
            return SupportedEncoding.UTF_8;
        }
        SupportedEncoding extensionEncoding = options.encoding.extensions.get(finalExtension(relativePath));
        if (extensionEncoding != null) {
            return extensionEncoding;
        }
        return options.encoding.defaultEncoding == null ? SupportedEncoding.UTF_8 : options.encoding.defaultEncoding;
    }

    private String finalExtension(String relativePath) {
        String normalized = PathUtils.toPosixPath(relativePath);
        int slashIndex = normalized.lastIndexOf('/');
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex <= slashIndex || dotIndex == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(dotIndex);
    }

    private String decodeText(byte[] bytes, SupportedEncoding encoding) {
        for (byte b : bytes) {
            if (b == 0) {
                return null;
            }
        }
        try {
            Charset charset = Charset.forName(encoding.charsetName);
            CharBuffer chars = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            return chars.toString();
        } catch (CharacterCodingException ex) {
            return null;
        }
    }

    private CollectedFile createCollectedFile(Path filePath, String relativePath, String content) {
        CollectedFile file = new CollectedFile();
        file.absolutePath = filePath.toString();
        file.relativePath = relativePath;
        file.extension = PathUtils.getExtension(filePath.toString());
        file.content = content;
        file.charCount = content.length();
        file.lineCount = content.length() == 0 ? 0 : content.split("\\r?\\n", -1).length;
        file.markers.addAll(extractMarkers(relativePath, content));
        return file;
    }

    private List<Marker> extractMarkers(String relativePath, String content) {
        List<Marker> markers = new ArrayList<Marker>();
        String[] lines = content.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = MARKER_PATTERN.matcher(lines[i]);
            if (matcher.find()) {
                Marker marker = new Marker();
                marker.relativePath = relativePath;
                marker.line = i + 1;
                marker.kind = matcher.group(1);
                marker.text = lines[i].trim();
                markers.add(marker);
            }
        }
        return markers;
    }

    private List<Marker> collectMarkers(List<CollectedFile> files) {
        List<Marker> markers = new ArrayList<Marker>();
        for (CollectedFile file : files) {
            markers.addAll(file.markers);
        }
        return markers;
    }

    private SkippedFile skippedForOversizedFile(String relativePath, int maxInputFileBytes) {
        SkippedFile file = new SkippedFile();
        file.relativePath = relativePath;
        file.reason = "ファイルサイズが " + maxInputFileBytes + " bytes の上限を超えたためスキップしました。";
        return file;
    }

    private SkippedFile skippedForUnreadableFile(String relativePath, SupportedEncoding encoding) {
        SkippedFile file = new SkippedFile();
        file.relativePath = relativePath;
        file.reason = encoding.displayName + " として読めない、またはバイナリと判定したためスキップしました。";
        return file;
    }

    private BundlePartsResult buildParts(List<CollectedFile> files, int maxChars) {
        BundleChunksResult chunksResult = buildChunks(files, maxChars);
        List<BundlePart> parts = new ArrayList<BundlePart>();
        List<BundleChunk> currentChunks = new ArrayList<BundleChunk>();
        int currentChars = 0;

        for (BundleChunk chunk : chunksResult.chunks) {
            if (!currentChunks.isEmpty() && currentChars + chunk.content.length() > maxChars) {
                parts.add(createBundlePart(parts.size() + 1, currentChunks, currentChars));
                currentChunks = new ArrayList<BundleChunk>();
                currentChars = 0;
            }
            currentChunks.add(chunk);
            currentChars += chunk.content.length();
        }

        if (!currentChunks.isEmpty()) {
            parts.add(createBundlePart(parts.size() + 1, currentChunks, currentChars));
        }

        return new BundlePartsResult(parts, chunksResult.warnings);
    }

    private BundleChunksResult buildChunks(List<CollectedFile> files, int maxChars) {
        List<BundleChunk> chunks = new ArrayList<BundleChunk>();
        List<String> warnings = new ArrayList<String>();
        for (CollectedFile file : files) {
            List<BundleChunk> fileChunks = splitOversizedFile(file, maxChars);
            if (fileChunks.size() > 1) {
                warnings.add("`" + file.relativePath + "` は --max-chars を超えたため " + fileChunks.size() + " 個に分割しました。");
            }
            chunks.addAll(fileChunks);
        }
        return new BundleChunksResult(chunks, warnings);
    }

    private List<BundleChunk> splitOversizedFile(CollectedFile file, int maxChars) {
        if (file.content.length() <= maxChars) {
            List<BundleChunk> chunks = new ArrayList<BundleChunk>();
            chunks.add(createSingleFileChunk(file));
            return chunks;
        }
        return createSplitFileChunks(file, splitContentByMaxChars(file.content, maxChars));
    }

    private List<String> splitContentByMaxChars(String content, int maxChars) {
        List<String> chunks = new ArrayList<String>();
        String current = "";
        String[] lines = splitAfterNewline(content);
        for (String line : lines) {
            if (current.length() > 0 && current.length() + line.length() > maxChars) {
                chunks.add(current);
                current = "";
            }
            if (line.length() > maxChars) {
                if (current.length() > 0) {
                    chunks.add(current);
                    current = "";
                }
                for (int i = 0; i < line.length(); i += maxChars) {
                    chunks.add(line.substring(i, Math.min(i + maxChars, line.length())));
                }
                continue;
            }
            current += line;
        }
        if (current.length() > 0) {
            chunks.add(current);
        }
        return chunks;
    }

    private String[] splitAfterNewline(String content) {
        List<String> lines = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lines.add(content.substring(start, i + 1));
                start = i + 1;
            }
        }
        if (start < content.length()) {
            lines.add(content.substring(start));
        }
        return lines.toArray(new String[lines.size()]);
    }

    private BundleChunk createSingleFileChunk(CollectedFile file) {
        BundleChunk chunk = new BundleChunk();
        chunk.relativePath = file.relativePath;
        chunk.extension = file.extension;
        chunk.content = file.content;
        chunk.originalCharCount = file.charCount;
        chunk.originalLineCount = file.lineCount;
        chunk.chunkIndex = 1;
        chunk.chunkCount = 1;
        return chunk;
    }

    private List<BundleChunk> createSplitFileChunks(CollectedFile file, List<String> chunkContents) {
        List<BundleChunk> chunks = new ArrayList<BundleChunk>();
        for (int i = 0; i < chunkContents.size(); i++) {
            BundleChunk chunk = new BundleChunk();
            chunk.relativePath = file.relativePath;
            chunk.extension = file.extension;
            chunk.content = chunkContents.get(i);
            chunk.originalCharCount = file.charCount;
            chunk.originalLineCount = file.lineCount;
            chunk.chunkIndex = i + 1;
            chunk.chunkCount = chunkContents.size();
            chunk.splitReason = "このファイルはサイズ上限を超えたため、やむを得ず分割しました。";
            chunks.add(chunk);
        }
        return chunks;
    }

    private BundlePart createBundlePart(int partNumber, List<BundleChunk> chunks, int charCount) {
        BundlePart part = new BundlePart();
        part.fileName = "text-bundle-" + String.format("%03d", partNumber) + ".md";
        part.partNumber = partNumber;
        part.chunks.addAll(chunks);
        part.charCount = charCount;
        return part;
    }

    private BundleMarkdownPaths writeBundleMarkdownFiles(Path outputDirectory, Path inputDirectory, List<BundlePart> parts,
            List<CollectedFile> collectedFiles, List<SkippedFile> skippedFiles, List<Marker> markers, List<String> warnings)
            throws IOException {
        Path indexPath = outputDirectory.resolve(INDEX_FILE_NAME);
        Path promptPath = outputDirectory.resolve(PROMPT_FILE_NAME);
        List<String> partPaths = new ArrayList<String>();
        List<String> partFileNames = new ArrayList<String>();

        for (BundlePart part : parts) {
            Path partPath = outputDirectory.resolve(part.fileName);
            Files.write(partPath, Markdown.buildPartMarkdown(part).getBytes(StandardCharsets.UTF_8));
            partPaths.add(partPath.toString());
            partFileNames.add(part.fileName);
        }

        Files.write(indexPath, Markdown.buildIndexMarkdown(inputDirectory.toString(), outputDirectory.toString(), parts,
                collectedFiles, skippedFiles, markers, warnings).getBytes(StandardCharsets.UTF_8));
        Files.write(promptPath, Markdown.buildPromptMarkdown(partFileNames).getBytes(StandardCharsets.UTF_8));

        return new BundleMarkdownPaths(indexPath.toString(), promptPath.toString(), partPaths);
    }

    private String relativeInputPath(Path inputPath, Path filePath) {
        return PathUtils.toPosixPath(inputPath.relativize(filePath).toString());
    }

    private static final class CollectedFilesResult {
        private final List<CollectedFile> files;
        private final List<SkippedFile> skipped;
        private final IgnoreStats ignored;

        private CollectedFilesResult(List<CollectedFile> files, List<SkippedFile> skipped, IgnoreStats ignored) {
            this.files = files;
            this.skipped = skipped;
            this.ignored = ignored;
        }
    }

    private static final class BundleChunksResult {
        private final List<BundleChunk> chunks;
        private final List<String> warnings;

        private BundleChunksResult(List<BundleChunk> chunks, List<String> warnings) {
            this.chunks = chunks;
            this.warnings = warnings;
        }
    }

    private static final class BundlePartsResult {
        private final List<BundlePart> parts;
        private final List<String> warnings;

        private BundlePartsResult(List<BundlePart> parts, List<String> warnings) {
            this.parts = parts;
            this.warnings = warnings;
        }
    }

    private static final class BundleMarkdownPaths {
        private final String indexPath;
        private final String promptPath;
        private final List<String> partPaths;

        private BundleMarkdownPaths(String indexPath, String promptPath, List<String> partPaths) {
            this.indexPath = indexPath;
            this.promptPath = promptPath;
            this.partPaths = partPaths;
        }
    }
}
