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
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.igapyon.mikutextbundle.discovery.DiscoveryResult;
import jp.igapyon.mikutextbundle.discovery.FileDiscovery;
import jp.igapyon.mikutextbundle.markdown.Markdown;
import jp.igapyon.mikutextbundle.match.PatternMatcher;
import jp.igapyon.mikutextbundle.model.BundleChunk;
import jp.igapyon.mikutextbundle.model.BundleMode;
import jp.igapyon.mikutextbundle.model.BundlePart;
import jp.igapyon.mikutextbundle.model.CliOptions;
import jp.igapyon.mikutextbundle.model.CollectedFile;
import jp.igapyon.mikutextbundle.model.IgnoreStats;
import jp.igapyon.mikutextbundle.model.Marker;
import jp.igapyon.mikutextbundle.model.SkippedFile;
import jp.igapyon.mikutextbundle.model.SupportedEncoding;
import jp.igapyon.mikutextbundle.pathutils.PathUtils;

public class TextBundler {
    private static final String DEFAULT_FILENAME_PREFIX = "text-bundle";
    private static final String DEFAULT_KNOWLEDGE_FILENAME_PREFIX = "knowledge";
    private static final int MAX_BUNDLE_PART_NUMBER = 999;
    private static final int EMBEDDED_SECTION_RESERVE_MARGIN_CHARS = 256;
    private static final double EMBEDDED_SECTION_RESERVE_MARGIN_RATIO = 0.1;
    private static final Pattern MARKER_PATTERN = Pattern.compile("\\b(TODO|FIXME|XXX)\\b(?!\\.)(.*)");

    public BundleResult createTextBundle(CliOptions options) throws IOException {
        return createTextBundle(options, new Date(), System.out);
    }

    public BundleResult createTextBundle(CliOptions options, Date now, PrintStream out) throws IOException {
        BundleMode mode = options.mode == null ? BundleMode.HANDOFF : options.mode;
        Path inputPath = Paths.get(options.inputDirectory).toAbsolutePath().normalize();
        if (!Files.isDirectory(inputPath)) {
            throw new IllegalArgumentException("Input directory does not exist: " + inputPath);
        }

        Path outputDirectory = chooseOutputDirectory(options.outputDirectory);
        String defaultPrefix = mode == BundleMode.KNOWLEDGE_SOURCE ? DEFAULT_KNOWLEDGE_FILENAME_PREFIX : DEFAULT_FILENAME_PREFIX;
        String filenamePrefix = normalizeFilenamePrefix(options.filenamePrefix == null ? defaultPrefix : options.filenamePrefix);
        if (!options.dryRun) {
            Files.createDirectories(outputDirectory);
        }

        List<String> gitignorePatterns = readRootGitignore(inputPath);
        CollectedFilesResult collected = collectFiles(inputPath, outputDirectory, options, gitignorePatterns);
        List<Marker> markers = collectMarkers(collected.files);
        BundlePartsResult partsResult = mode == BundleMode.KNOWLEDGE_SOURCE
                ? buildParts(collected.files, options.maxChars, filenamePrefix)
                : buildPartsWithEmbeddedReserves(collected.files, options.maxChars, filenamePrefix,
                        inputPath, outputDirectory, collected.skipped, markers);

        BundleMarkdownPaths paths;
        if (mode == BundleMode.KNOWLEDGE_SOURCE) {
            paths = options.dryRun
                    ? plannedKnowledgeMarkdownPaths(outputDirectory, filenamePrefix, partsResult.parts)
                    : writeKnowledgeMarkdownFiles(outputDirectory, filenamePrefix, inputPath, partsResult.parts,
                            collected.files, collected.skipped, markers, partsResult.warnings, options);
        } else {
            paths = options.dryRun ? plannedBundleMarkdownPaths(outputDirectory, filenamePrefix, partsResult.parts)
                    : writeBundleMarkdownFiles(outputDirectory, filenamePrefix, inputPath, partsResult.parts, collected.files,
                            collected.skipped, markers, partsResult.warnings);
        }
        List<String> resultWarnings = new ArrayList<String>(partsResult.warnings);
        for (String stale : paths.staleOutputCandidates) {
            resultWarnings.add("Stale generated output remains: `" + stale + "`.");
        }

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

        if (!options.dryRun) {
            for (String partPath : paths.partPaths) {
                out.println("generated: " + partPath);
            }
            if (mode == BundleMode.KNOWLEDGE_SOURCE) {
                out.println("generated: " + paths.indexPath);
            }
        }

        BundleResult result = new BundleResult();
        result.mode = mode;
        result.outputDirectory = outputDirectory.toString();
        result.indexPath = paths.indexPath;
        result.promptPath = paths.promptPath;
        result.partPaths.addAll(paths.partPaths);
        if (mode == BundleMode.KNOWLEDGE_SOURCE) {
            result.knowledgeSourcePaths.addAll(paths.partPaths);
            result.managementIndexPath = paths.indexPath;
        }
        result.filesCollected = collected.files.size();
        result.filesSkipped = collected.skipped.size();
        result.directoriesIgnored = collected.ignored.directories;
        result.filesIgnored = collected.ignored.files;
        result.ignoredByDirectory = collected.ignored.byDirectory;
        result.ignoredByExtension = collected.ignored.byExtension;
        result.ignoredByGitignore = collected.ignored.byGitignore;
        result.ignoredByOutputDirectory = collected.ignored.byOutputDirectory;
        result.partsGenerated = partsResult.parts.size();
        result.warnings.addAll(resultWarnings);
        result.dryRun = options.dryRun;
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
        file.reason = "File size exceeds the " + maxInputFileBytes + " byte limit.";
        return file;
    }

    private SkippedFile skippedForUnreadableFile(String relativePath, SupportedEncoding encoding) {
        SkippedFile file = new SkippedFile();
        file.relativePath = relativePath;
        file.reason = "Skipped because the file cannot be decoded as " + encoding.displayName + " or was detected as binary.";
        return file;
    }

    private BundlePartsResult buildParts(List<CollectedFile> files, int maxChars, String filenamePrefix) {
        return buildParts(files, maxChars, filenamePrefix, new BundlePartReserves(0, 0));
    }

    private BundlePartsResult buildParts(List<CollectedFile> files, int maxChars, String filenamePrefix, BundlePartReserves reserves) {
        BundleChunksResult chunksResult = buildChunks(files, maxChars);
        List<BundlePart> parts = new ArrayList<BundlePart>();
        List<BundleChunk> currentChunks = new ArrayList<BundleChunk>();
        int currentChars = 0;

        for (BundleChunk chunk : chunksResult.chunks) {
            int currentPartNumber = parts.size() + 1;
            int currentMaxChars = effectiveMaxChars(maxChars, currentPartNumber == 1 ? reserves.firstPartChars : 0);
            if (!currentChunks.isEmpty() && currentChars + chunk.content.length() > currentMaxChars) {
                parts.add(createBundlePart(filenamePrefix, parts.size() + 1, currentChunks, currentChars));
                currentChunks = new ArrayList<BundleChunk>();
                currentChars = 0;
            }
            currentChunks.add(chunk);
            currentChars += chunk.content.length();
        }

        if (!currentChunks.isEmpty()) {
            parts.add(createBundlePart(filenamePrefix, parts.size() + 1, currentChunks, currentChars));
        }
        if (parts.isEmpty()) {
            parts.add(createBundlePart(filenamePrefix, 1, new ArrayList<BundleChunk>(), 0));
        }

        return new BundlePartsResult(shrinkLastPartForReserve(parts, maxChars, filenamePrefix, reserves.lastPartChars),
                chunksResult.warnings);
    }

    private BundleChunksResult buildChunks(List<CollectedFile> files, int maxChars) {
        List<BundleChunk> chunks = new ArrayList<BundleChunk>();
        List<String> warnings = new ArrayList<String>();
        for (CollectedFile file : files) {
            List<BundleChunk> fileChunks = splitOversizedFile(file, maxChars);
            if (fileChunks.size() > 1) {
                warnings.add("`" + file.relativePath + "` exceeded --max-chars and was split into " + fileChunks.size() + " chunks.");
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
        chunk.sourceStartLine = file.lineCount == 0 ? 0 : 1;
        chunk.sourceEndLine = file.lineCount;
        chunk.sourceStartChar = 0;
        chunk.sourceEndChar = file.content.length();
        return chunk;
    }

    private List<BundleChunk> createSplitFileChunks(CollectedFile file, List<String> chunkContents) {
        List<BundleChunk> chunks = new ArrayList<BundleChunk>();
        int sourceStartChar = 0;
        for (int i = 0; i < chunkContents.size(); i++) {
            BundleChunk chunk = new BundleChunk();
            chunk.relativePath = file.relativePath;
            chunk.extension = file.extension;
            chunk.content = chunkContents.get(i);
            chunk.originalCharCount = file.charCount;
            chunk.originalLineCount = file.lineCount;
            chunk.chunkIndex = i + 1;
            chunk.chunkCount = chunkContents.size();
            int sourceEndChar = sourceStartChar + chunk.content.length();
            chunk.sourceStartLine = sourceLineAtOffset(file.content, sourceStartChar);
            chunk.sourceEndLine = sourceLineAtOffset(file.content, Math.max(sourceStartChar, sourceEndChar - 1));
            chunk.sourceStartChar = sourceStartChar;
            chunk.sourceEndChar = sourceEndChar;
            chunk.splitReason = "This file exceeded the size limit and was split.";
            chunks.add(chunk);
            sourceStartChar = sourceEndChar;
        }
        return chunks;
    }

    private int sourceLineAtOffset(String content, int offset) {
        if (content.length() == 0) return 0;
        int bounded = Math.max(0, Math.min(offset, content.length() - 1));
        int line = 1;
        for (int i = 0; i < bounded; i++) if (content.charAt(i) == '\n') line++;
        return line;
    }

    private BundlePart createBundlePart(String filenamePrefix, int partNumber, List<BundleChunk> chunks, int charCount) {
        if (partNumber > MAX_BUNDLE_PART_NUMBER) {
            throw new IllegalArgumentException("Part count exceeds " + MAX_BUNDLE_PART_NUMBER
                    + "; only three-digit part file names are supported.");
        }

        BundlePart part = new BundlePart();
        part.fileName = bundlePartFileName(filenamePrefix, partNumber);
        part.partNumber = partNumber;
        part.chunks.addAll(chunks);
        part.charCount = charCount;
        return part;
    }

    private int chunksCharCount(List<BundleChunk> chunks) {
        int total = 0;
        for (BundleChunk chunk : chunks) {
            total += chunk.content.length();
        }
        return total;
    }

    private int effectiveMaxChars(int maxChars, int reservedChars) {
        return Math.max(1, maxChars - reservedChars);
    }

    private List<BundlePart> shrinkLastPartForReserve(List<BundlePart> parts, int maxChars, String filenamePrefix,
            int lastPartReservedChars) {
        int lastPartMaxChars = effectiveMaxChars(maxChars, lastPartReservedChars);
        List<BundlePart> adjustedParts = cloneParts(parts);

        while (!adjustedParts.isEmpty()) {
            BundlePart lastPart = adjustedParts.get(adjustedParts.size() - 1);
            if (lastPart.charCount <= lastPartMaxChars || lastPart.chunks.size() <= 1) {
                break;
            }

            List<BundleChunk> movedChunks = new ArrayList<BundleChunk>();
            int movedCharCount = 0;
            while (lastPart.charCount > lastPartMaxChars && lastPart.chunks.size() > 1) {
                BundleChunk movedChunk = lastPart.chunks.remove(lastPart.chunks.size() - 1);
                movedChunks.add(0, movedChunk);
                movedCharCount += movedChunk.content.length();
                lastPart.charCount -= movedChunk.content.length();
            }
            adjustedParts.add(createBundlePart(filenamePrefix, adjustedParts.size() + 1, movedChunks, movedCharCount));
        }

        return renumberParts(adjustedParts, filenamePrefix);
    }

    private List<BundlePart> cloneParts(List<BundlePart> parts) {
        List<BundlePart> cloned = new ArrayList<BundlePart>();
        for (BundlePart part : parts) {
            cloned.add(createBundlePartFromExisting(part.fileName, part.partNumber, part.chunks, part.charCount));
        }
        return cloned;
    }

    private List<BundlePart> renumberParts(List<BundlePart> parts, String filenamePrefix) {
        List<BundlePart> renumbered = new ArrayList<BundlePart>();
        for (int i = 0; i < parts.size(); i++) {
            BundlePart part = parts.get(i);
            renumbered.add(createBundlePart(filenamePrefix, i + 1, part.chunks, chunksCharCount(part.chunks)));
        }
        return renumbered;
    }

    private BundlePart createBundlePartFromExisting(String fileName, int partNumber, List<BundleChunk> chunks, int charCount) {
        BundlePart part = new BundlePart();
        part.fileName = fileName;
        part.partNumber = partNumber;
        part.chunks.addAll(chunks);
        part.charCount = charCount;
        return part;
    }

    private int withReserveSafetyMargin(int charCount) {
        return (int) Math.ceil(charCount * (1 + EMBEDDED_SECTION_RESERVE_MARGIN_RATIO)) + EMBEDDED_SECTION_RESERVE_MARGIN_CHARS;
    }

    private BundlePartsResult buildPartsWithEmbeddedReserves(List<CollectedFile> files, int maxChars, String filenamePrefix,
            Path inputDirectory, Path outputDirectory, List<SkippedFile> skippedFiles, List<Marker> markers) {
        BundlePartsResult result = buildParts(files, maxChars, filenamePrefix);

        for (int i = 0; i < 5; i++) {
            BundlePartReserves reserves = new BundlePartReserves(
                    withReserveSafetyMargin(estimateEmbeddedPromptChars(result.parts, filenamePrefix)),
                    withReserveSafetyMargin(estimateEmbeddedIndexChars(filenamePrefix, inputDirectory, outputDirectory,
                            result.parts, files, skippedFiles, markers, result.warnings)));
            BundlePartsResult nextResult = buildParts(files, maxChars, filenamePrefix, reserves);
            if (nextResult.parts.size() == result.parts.size()) {
                return nextResult;
            }
            result = nextResult;
        }

        return result;
    }

    private int estimateEmbeddedPromptChars(List<BundlePart> parts, String filenamePrefix) {
        String promptFileName = bundlePromptFileName(filenamePrefix);
        String indexFileName = parts.isEmpty() ? promptFileName : parts.get(parts.size() - 1).fileName;
        BundlePart emptyPart = createBundlePart(filenamePrefix, 1, new ArrayList<BundleChunk>(), 0);
        Markdown.PromptOptions prompt = new Markdown.PromptOptions(promptFileName, partFileNames(parts), indexFileName);
        return Markdown.buildPartMarkdown(emptyPart, prompt, null).length() - Markdown.buildPartMarkdown(emptyPart).length();
    }

    private int estimateEmbeddedIndexChars(String filenamePrefix, Path inputDirectory, Path outputDirectory, List<BundlePart> parts,
            List<CollectedFile> collectedFiles, List<SkippedFile> skippedFiles, List<Marker> markers, List<String> warnings) {
        int lastPartNumber = Math.max(parts.size(), 1);
        BundlePart emptyPart = createBundlePart(filenamePrefix, lastPartNumber, new ArrayList<BundleChunk>(), 0);
        String terminalFileName = parts.isEmpty() ? bundlePromptFileName(filenamePrefix) : parts.get(parts.size() - 1).fileName;
        Markdown.IndexOptions index = new Markdown.IndexOptions(displayPathFromCurrentDirectory(inputDirectory),
                displayPathFromCurrentDirectory(outputDirectory), parts, collectedFiles, skippedFiles, markers, warnings,
                terminalFileName);
        return Markdown.buildPartMarkdown(emptyPart, null, index).length() - Markdown.buildPartMarkdown(emptyPart).length();
    }

    private BundleMarkdownPaths writeBundleMarkdownFiles(Path outputDirectory, String filenamePrefix, Path inputDirectory, List<BundlePart> parts,
            List<CollectedFile> collectedFiles, List<SkippedFile> skippedFiles, List<Marker> markers, List<String> warnings)
            throws IOException {
        String promptFileName = bundlePromptFileName(filenamePrefix);
        String indexFileName = parts.isEmpty() ? promptFileName : parts.get(parts.size() - 1).fileName;
        Path indexPath = outputDirectory.resolve(indexFileName);
        Path promptPath = outputDirectory.resolve(promptFileName);
        List<String> partPaths = new ArrayList<String>();
        List<String> partFileNames = new ArrayList<String>();
        for (BundlePart part : parts) {
            partFileNames.add(part.fileName);
        }

        for (int i = 0; i < parts.size(); i++) {
            BundlePart part = parts.get(i);
            Path partPath = outputDirectory.resolve(part.fileName);
            Files.write(partPath, buildRenderedPartMarkdown(outputDirectory, filenamePrefix, inputDirectory, parts,
                    collectedFiles, skippedFiles, markers, warnings, i).getBytes(StandardCharsets.UTF_8));
            partPaths.add(partPath.toString());
        }

        return new BundleMarkdownPaths(indexPath.toString(), promptPath.toString(), partPaths);
    }

    private BundleMarkdownPaths plannedBundleMarkdownPaths(Path outputDirectory, String filenamePrefix, List<BundlePart> parts) {
        String promptFileName = bundlePromptFileName(filenamePrefix);
        String indexFileName = parts.isEmpty() ? promptFileName : parts.get(parts.size() - 1).fileName;
        Path indexPath = outputDirectory.resolve(indexFileName);
        Path promptPath = outputDirectory.resolve(promptFileName);
        List<String> partPaths = new ArrayList<String>();
        for (BundlePart part : parts) {
            partPaths.add(outputDirectory.resolve(part.fileName).toString());
        }
        return new BundleMarkdownPaths(indexPath.toString(), promptPath.toString(), partPaths);
    }

    private BundleMarkdownPaths writeKnowledgeMarkdownFiles(Path outputDirectory, String filenamePrefix, Path inputDirectory,
            List<BundlePart> parts, List<CollectedFile> collectedFiles, List<SkippedFile> skippedFiles,
            List<Marker> markers, List<String> warnings, CliOptions options) throws IOException {
        List<String> stale = staleKnowledgeOutputs(outputDirectory, filenamePrefix, parts);
        List<String> partPaths = new ArrayList<String>();
        for (BundlePart part : parts) {
            Path path = outputDirectory.resolve(part.fileName);
            Files.write(path, Markdown.buildKnowledgeSourceMarkdown(part).getBytes(StandardCharsets.UTF_8));
            partPaths.add(path.toString());
        }
        String indexFileName = filenamePrefix + "-index.md";
        Path indexPath = outputDirectory.resolve(indexFileName);
        Markdown.KnowledgeIndexOptions index = knowledgeIndexOptions(indexFileName, inputDirectory, outputDirectory,
                filenamePrefix, options, parts, collectedFiles, skippedFiles, markers, warnings, stale);
        Files.write(indexPath, Markdown.buildKnowledgeIndexMarkdown(index).getBytes(StandardCharsets.UTF_8));
        return new BundleMarkdownPaths(indexPath.toString(), partPaths.get(0), partPaths, stale);
    }

    private BundleMarkdownPaths plannedKnowledgeMarkdownPaths(Path outputDirectory, String filenamePrefix,
            List<BundlePart> parts) throws IOException {
        List<String> partPaths = new ArrayList<String>();
        for (BundlePart part : parts) partPaths.add(outputDirectory.resolve(part.fileName).toString());
        return new BundleMarkdownPaths(outputDirectory.resolve(filenamePrefix + "-index.md").toString(),
                partPaths.get(0), partPaths, staleKnowledgeOutputs(outputDirectory, filenamePrefix, parts));
    }

    private List<String> staleKnowledgeOutputs(Path outputDirectory, String filenamePrefix, List<BundlePart> parts)
            throws IOException {
        List<String> result = new ArrayList<String>();
        if (!Files.isDirectory(outputDirectory)) return result;
        List<String> planned = new ArrayList<String>();
        for (BundlePart part : parts) planned.add(part.fileName);
        Pattern pattern = Pattern.compile("^" + Pattern.quote(filenamePrefix) + "-\\d{3}\\.md$");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputDirectory)) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                if (pattern.matcher(name).matches() && !planned.contains(name)) result.add(name);
            }
        }
        Collections.sort(result);
        return result;
    }

    private Markdown.KnowledgeIndexOptions knowledgeIndexOptions(String indexFileName, Path inputDirectory,
            Path outputDirectory, String filenamePrefix, CliOptions options, List<BundlePart> parts,
            List<CollectedFile> collectedFiles, List<SkippedFile> skippedFiles, List<Marker> markers,
            List<String> warnings, List<String> stale) {
        Markdown.KnowledgeIndexOptions index = new Markdown.KnowledgeIndexOptions();
        index.managementIndexFileName = indexFileName;
        index.configuration.add(new String[] { "mode", "knowledge-source" });
        index.configuration.add(new String[] { "input", displayPathFromCurrentDirectory(inputDirectory) });
        index.configuration.add(new String[] { "output", displayPathFromCurrentDirectory(outputDirectory) });
        index.configuration.add(new String[] { "filename-prefix", filenamePrefix });
        index.configuration.add(new String[] { "max-chars", String.valueOf(options.maxChars) });
        index.configuration.add(new String[] { "max-input-file-bytes", String.valueOf(options.maxInputFileBytes) });
        index.configuration.add(new String[] { "encoding", options.encoding.defaultEncoding.optionValue });
        index.configuration.add(new String[] { "encoding-extension", sortedEncodingExtensions(options) });
        index.configuration.add(new String[] { "exclude-extensions", sortedValues(options.excludeExtensions) });
        index.configuration.add(new String[] { "exclude-directories", sortedValues(options.excludeDirectories) });
        index.parts.addAll(parts); index.collectedFiles.addAll(collectedFiles); index.skippedFiles.addAll(skippedFiles);
        index.markers.addAll(markers); index.warnings.addAll(warnings); index.staleOutputCandidates.addAll(stale);
        return index;
    }

    private String sortedEncodingExtensions(CliOptions options) {
        List<String> values = new ArrayList<String>();
        for (java.util.Map.Entry<String, SupportedEncoding> entry : options.encoding.extensions.entrySet())
            values.add(entry.getKey() + "=" + entry.getValue().optionValue);
        Collections.sort(values);
        return joinValues(values);
    }

    private String sortedValues(List<String> source) {
        if (source == null) return "";
        List<String> values = new ArrayList<String>(source);
        Collections.sort(values);
        return joinValues(values);
    }

    private String joinValues(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) { if (result.length() > 0) result.append(", "); result.append(value); }
        return result.toString();
    }

    private String buildRenderedPartMarkdown(Path outputDirectory, String filenamePrefix, Path inputDirectory,
            List<BundlePart> parts, List<CollectedFile> collectedFiles, List<SkippedFile> skippedFiles, List<Marker> markers,
            List<String> warnings, int partIndex) {
        String promptFileName = bundlePromptFileName(filenamePrefix);
        String indexFileName = parts.isEmpty() ? promptFileName : parts.get(parts.size() - 1).fileName;
        List<String> partFileNames = partFileNames(parts);
        BundlePart part = parts.get(partIndex);
        Markdown.PromptOptions prompt = partIndex == 0
                ? new Markdown.PromptOptions(promptFileName, partFileNames, indexFileName)
                : null;
        Markdown.IndexOptions index = partIndex == parts.size() - 1
                ? new Markdown.IndexOptions(displayPathFromCurrentDirectory(inputDirectory),
                        displayPathFromCurrentDirectory(outputDirectory), parts, collectedFiles, skippedFiles, markers,
                        warnings, indexFileName)
                : null;
        return Markdown.buildPartMarkdown(part, prompt, index, partIndex != parts.size() - 1);
    }

    private List<String> partFileNames(List<BundlePart> parts) {
        List<String> names = new ArrayList<String>();
        for (BundlePart part : parts) {
            names.add(part.fileName);
        }
        return names;
    }

    private String displayPathFromCurrentDirectory(Path path) {
        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();
        try {
            Path relativePath = currentDirectory.relativize(path.toAbsolutePath().normalize());
            String value = relativePath.toString();
            if (value.length() == 0) {
                return ".";
            }
            return PathUtils.toPosixPath(value);
        } catch (IllegalArgumentException ex) {
            return PathUtils.toPosixPath(path.toString());
        }
    }

    private String normalizeFilenamePrefix(String value) {
        String prefix = value == null ? DEFAULT_FILENAME_PREFIX : value.trim();
        if (prefix.length() == 0) {
            throw new IllegalArgumentException("filenamePrefix must not be empty.");
        }
        if (!prefix.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "filenamePrefix must contain only ASCII letters, digits, dots, underscores, and hyphens.");
        }
        return prefix;
    }

    private String bundlePromptFileName(String filenamePrefix) {
        return filenamePrefix + "-001.md";
    }

    private String bundlePartFileName(String filenamePrefix, int partNumber) {
        return filenamePrefix + "-" + String.format("%03d", partNumber) + ".md";
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

    private static final class BundlePartReserves {
        private final int firstPartChars;
        private final int lastPartChars;

        private BundlePartReserves(int firstPartChars, int lastPartChars) {
            this.firstPartChars = firstPartChars;
            this.lastPartChars = lastPartChars;
        }
    }

    private static final class BundleMarkdownPaths {
        private final String indexPath;
        private final String promptPath;
        private final List<String> partPaths;
        private final List<String> staleOutputCandidates;

        private BundleMarkdownPaths(String indexPath, String promptPath, List<String> partPaths) {
            this(indexPath, promptPath, partPaths, new ArrayList<String>());
        }

        private BundleMarkdownPaths(String indexPath, String promptPath, List<String> partPaths, List<String> staleOutputCandidates) {
            this.indexPath = indexPath;
            this.promptPath = promptPath;
            this.partPaths = partPaths;
            this.staleOutputCandidates = staleOutputCandidates;
        }
    }
}
