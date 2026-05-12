package jp.igapyon.mikutextbundle.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jp.igapyon.mikutextbundle.coreapi.BundleResult;
import jp.igapyon.mikutextbundle.coreapi.TextBundler;
import jp.igapyon.mikutextbundle.core.MikuTextBundle;
import jp.igapyon.mikutextbundle.discovery.FileDiscovery;
import jp.igapyon.mikutextbundle.model.EncodingOptions;
import jp.igapyon.mikutextbundle.model.CliOptions;
import jp.igapyon.mikutextbundle.model.SupportedEncoding;
import jp.igapyon.mikutextbundle.pathutils.PathUtils;

/**
 * Command line entrypoint for miku-text-bundle-java.
 */
public final class MikuTextBundleCli {
    private MikuTextBundleCli() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        System.exit(exitCode);
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            CliOptions options = parseArgs(args);
            ByteArrayOutputStreamBridge generatedOutput = new ByteArrayOutputStreamBridge();
            BundleResult result = new TextBundler().createTextBundle(options, new java.util.Date(), generatedOutput.printStream);
            out.print(generatedOutput.text());
            out.println("completed: " + result.partsGenerated + " part(s), " + result.filesCollected
                    + " file(s) collected, " + result.filesSkipped + " file(s) skipped, "
                    + result.directoriesIgnored + " directories ignored, " + result.filesIgnored + " file(s) ignored");
            return 0;
        } catch (HelpRequestedException ex) {
            printHelp(out);
            return 0;
        } catch (VersionRequestedException ex) {
            out.println(MikuTextBundle.VERSION);
            return 0;
        } catch (Exception ex) {
            err.println("error: " + ex.getMessage());
            printHelp(out);
            return 1;
        }
    }

    public static CliOptions parseArgs(String[] argv) throws HelpRequestedException, VersionRequestedException {
        ParseState state = createParseState();

        for (int i = 0; i < argv.length; i++) {
            i = consumeOption(argv, i, state);
        }

        validateRequiredDirectories(state);

        CliOptions options = new CliOptions();
        options.inputDirectory = state.inputDirectory;
        options.outputDirectory = state.outputDirectory;
        options.maxChars = state.maxChars;
        options.maxInputFileBytes = state.maxInputFileBytes;
        options.encoding = state.encoding;
        options.excludeExtensions = sortedList(state.excludeExtensions);
        options.excludeDirectories = sortedList(state.excludeDirectories);
        options.verbose = state.verbose;
        return options;
    }

    public static void printHelp(PrintStream out) {
        out.println("Usage:");
        out.println("  miku-text-bundle --input <dir> --output <dir> [options]");
        out.println("  miku-text-bundle --help");
        out.println("  miku-text-bundle --version");
        out.println();
        out.println("Options:");
        out.println("  --max-chars <number>");
        out.println("  --max-input-file-bytes <number>");
        out.println("  --encoding utf-8|shift_jis");
        out.println("  --encoding-extension \".java=shift_jis\"");
        out.println("  --add-exclude-extension \".ext\"");
        out.println("  --remove-exclude-extension \".ext\"");
        out.println("  --add-exclude-directory \"dir\"");
        out.println("  --remove-exclude-directory \"dir\"");
        out.println("  --verbose");
        out.println();
        out.println("Description:");
        out.println("  Collect text-like files under the input directory and generate split");
        out.println("  Markdown bundles for generative AI handoff.");
    }

    private static ParseState createParseState() {
        ParseState state = new ParseState();
        state.maxChars = 120000;
        state.maxInputFileBytes = 1000000;
        state.encoding = new EncodingOptions();
        state.excludeExtensions = new LinkedHashSet<String>(FileDiscovery.DEFAULT_EXCLUDE_EXTENSIONS);
        state.excludeDirectories = new LinkedHashSet<String>(FileDiscovery.DEFAULT_EXCLUDE_DIRECTORIES);
        return state;
    }

    private static int consumeOption(String[] argv, int index, ParseState state)
            throws HelpRequestedException, VersionRequestedException {
        String arg = argv[index];

        if ("--help".equals(arg)) {
            throw new HelpRequestedException();
        }

        if ("--version".equals(arg)) {
            throw new VersionRequestedException();
        }

        if ("--input".equals(arg)) {
            state.inputDirectory = readRequiredOptionValue(argv, index, "--input");
            return index + 1;
        }

        if ("--output".equals(arg)) {
            state.outputDirectory = readRequiredOptionValue(argv, index, "--output");
            return index + 1;
        }

        if ("--max-chars".equals(arg)) {
            state.maxChars = parsePositiveInteger(readRequiredOptionValue(argv, index, "--max-chars"), "--max-chars");
            return index + 1;
        }

        if ("--max-input-file-bytes".equals(arg)) {
            state.maxInputFileBytes = parsePositiveInteger(readRequiredOptionValue(argv, index, "--max-input-file-bytes"),
                    "--max-input-file-bytes");
            return index + 1;
        }

        if ("--encoding".equals(arg)) {
            state.encoding.defaultEncoding = SupportedEncoding.parse(readRequiredOptionValue(argv, index, "--encoding"),
                    "--encoding");
            return index + 1;
        }

        if ("--encoding-extension".equals(arg)) {
            state.encoding.extensions.putAll(parseEncodingExtensions(readRequiredOptionValue(argv, index,
                    "--encoding-extension")));
            return index + 1;
        }

        if ("--add-exclude-extension".equals(arg)) {
            for (String extension : parseExtensionList(readRequiredOptionValue(argv, index, "--add-exclude-extension"),
                    "--add-exclude-extension")) {
                state.excludeExtensions.add(extension);
            }
            return index + 1;
        }

        if ("--remove-exclude-extension".equals(arg)) {
            for (String extension : parseExtensionList(readRequiredOptionValue(argv, index,
                    "--remove-exclude-extension"), "--remove-exclude-extension")) {
                state.excludeExtensions.remove(extension);
            }
            return index + 1;
        }

        if ("--add-exclude-directory".equals(arg)) {
            for (String directory : parseDirectoryList(readRequiredOptionValue(argv, index, "--add-exclude-directory"),
                    "--add-exclude-directory")) {
                state.excludeDirectories.add(directory);
            }
            return index + 1;
        }

        if ("--remove-exclude-directory".equals(arg)) {
            for (String directory : parseDirectoryList(readRequiredOptionValue(argv, index,
                    "--remove-exclude-directory"), "--remove-exclude-directory")) {
                state.excludeDirectories.remove(directory);
            }
            return index + 1;
        }

        if ("--verbose".equals(arg)) {
            state.verbose = true;
            return index;
        }

        if (arg.startsWith("-")) {
            throw new IllegalArgumentException("Unknown argument: " + arg);
        }

        throw new IllegalArgumentException("Positional arguments are not supported. Use --input and --output: " + arg);
    }

    private static String readRequiredOptionValue(String[] argv, int index, String optionName) {
        if (index + 1 >= argv.length || argv[index + 1].startsWith("--")) {
            throw new IllegalArgumentException("Please specify a value for " + optionName + ".");
        }
        return argv[index + 1];
    }

    private static List<String> parsePatternList(String value) {
        List<String> patterns = new ArrayList<String>();
        String[] items = value.split(",");
        for (String item : items) {
            String trimmed = item.trim();
            if (trimmed.length() > 0) {
                patterns.add(trimmed);
            }
        }
        return patterns;
    }

    private static List<String> parseExtensionList(String value, String optionName) {
        List<String> extensions = new ArrayList<String>();
        for (String item : parsePatternList(value)) {
            String extension = item.toLowerCase();
            if (extension.length() <= 1 || !extension.startsWith(".") || extension.indexOf('/') >= 0
                    || extension.indexOf('\\') >= 0) {
                throw new IllegalArgumentException(optionName + " values must be extensions with a leading dot.");
            }
            extensions.add(extension);
        }
        return extensions;
    }

    private static List<String> parseDirectoryList(String value, String optionName) {
        List<String> directories = new ArrayList<String>();
        for (String item : parsePatternList(value)) {
            String directory = PathUtils.normalizePattern(item).replaceAll("/+$", "");
            if (directory.length() == 0 || ".".equals(directory)) {
                throw new IllegalArgumentException(optionName + " values must be relative directory names or paths.");
            }
            directories.add(directory);
        }
        return directories;
    }

    private static java.util.Map<String, SupportedEncoding> parseEncodingExtensions(String value) {
        java.util.Map<String, SupportedEncoding> extensions = new java.util.LinkedHashMap<String, SupportedEncoding>();
        for (String item : parsePatternList(value)) {
            int separatorIndex = item.indexOf('=');
            if (separatorIndex <= 0 || separatorIndex == item.length() - 1) {
                throw new IllegalArgumentException("--encoding-extension entries must use .ext=encoding format.");
            }

            String extension = item.substring(0, separatorIndex).trim();
            String encoding = item.substring(separatorIndex + 1).trim();
            if (!extension.startsWith(".") || extension.indexOf('/') >= 0 || extension.indexOf('\\') >= 0) {
                throw new IllegalArgumentException("--encoding-extension keys must be exact extensions with a leading dot.");
            }
            extensions.put(extension, SupportedEncoding.parse(encoding, "--encoding-extension"));
        }
        return extensions;
    }

    private static int parsePositiveInteger(String value, String optionName) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(optionName + " must be a positive integer.");
        }
    }

    private static void validateRequiredDirectories(ParseState state) {
        if (state.inputDirectory == null) {
            throw new IllegalArgumentException("Please specify --input.");
        }
        if (state.outputDirectory == null) {
            throw new IllegalArgumentException("Please specify --output.");
        }
    }

    private static List<String> sortedList(Set<String> values) {
        List<String> result = new ArrayList<String>(values);
        Collections.sort(result, new Comparator<String>() {
            public int compare(String left, String right) {
                return left.compareTo(right);
            }
        });
        return result;
    }

    private static final class ParseState {
        private String inputDirectory;
        private String outputDirectory;
        private int maxChars;
        private int maxInputFileBytes;
        private EncodingOptions encoding;
        private Set<String> excludeExtensions;
        private Set<String> excludeDirectories;
        private boolean verbose;
    }

    private static final class VersionRequestedException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static final class ByteArrayOutputStreamBridge {
        private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        private final PrintStream printStream = new PrintStream(bytes);

        private String text() {
            printStream.flush();
            return new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
