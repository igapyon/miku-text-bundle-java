package jp.igapyon.mikutextbundle.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import jp.igapyon.mikutextbundle.coreapi.BundleResult;
import jp.igapyon.mikutextbundle.coreapi.TextBundler;
import jp.igapyon.mikutextbundle.core.MikuTextBundle;
import jp.igapyon.mikutextbundle.model.EncodingOptions;
import jp.igapyon.mikutextbundle.model.CliOptions;
import jp.igapyon.mikutextbundle.model.SupportedEncoding;

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
            out.println("completed: " + result.partsGenerated + " part(s), " + result.filesCollected + " file(s) collected");
            return 0;
        } catch (HelpRequestedException ex) {
            printHelp(out);
            return 0;
        } catch (VersionRequestedException ex) {
            out.println(MikuTextBundle.productName() + " " + MikuTextBundle.VERSION);
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

        applyPositionalDirectories(state);
        if (state.inputDirectory == null) {
            throw new IllegalArgumentException("Please specify an input directory.");
        }

        CliOptions options = new CliOptions();
        options.inputDirectory = state.inputDirectory;
        options.outputDirectory = state.outputDirectory;
        options.maxChars = state.maxChars;
        options.maxInputFileBytes = state.maxInputFileBytes;
        options.encoding = state.encoding;
        options.includePatterns = state.includePatterns;
        options.excludePatterns = state.excludePatterns;
        options.verbose = state.verbose;
        return options;
    }

    public static void printHelp(PrintStream out) {
        out.println("Usage:");
        out.println("  miku-text-bundle <inputDir> [outputDir] [--max-chars 120000] [--max-input-file-bytes 1000000] [--encoding utf-8|shift_jis] [--encoding-extension \".java=shift_jis\"] [--include \"glob\"] [--exclude \"glob\"] [--verbose]");
        out.println("  miku-text-bundle --input-directory <dir> [--output-directory <dir>] [--max-chars 120000] [--max-input-file-bytes 1000000] [--encoding utf-8|shift_jis]");
        out.println();
        out.println("Description:");
        out.println("  Collect repository text files and generate split Markdown bundles for");
        out.println("  generative AI handoff. When outputDir is omitted, outputs are written under");
        out.println("  workplace/miku-text-bundle/<yyyyMMddHHmm>/.");
    }

    private static ParseState createParseState() {
        ParseState state = new ParseState();
        state.maxChars = 120000;
        state.maxInputFileBytes = 1000000;
        state.encoding = new EncodingOptions();
        state.includePatterns = new ArrayList<String>();
        state.excludePatterns = new ArrayList<String>();
        state.positional = new ArrayList<String>();
        return state;
    }

    private static int consumeOption(String[] argv, int index, ParseState state)
            throws HelpRequestedException, VersionRequestedException {
        String arg = argv[index];

        if ("--help".equals(arg) || "-h".equals(arg)) {
            throw new HelpRequestedException();
        }

        if ("--version".equals(arg)) {
            throw new VersionRequestedException();
        }

        if ("--input-directory".equals(arg)) {
            state.inputDirectory = readRequiredOptionValue(argv, index, "--input-directory");
            return index + 1;
        }

        if ("--output-directory".equals(arg)) {
            state.outputDirectory = readRequiredOptionValue(argv, index, "--output-directory");
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

        if ("--include".equals(arg)) {
            state.includePatterns = parsePatternList(readRequiredOptionValue(argv, index, "--include"));
            return index + 1;
        }

        if ("--exclude".equals(arg)) {
            state.excludePatterns = parsePatternList(readRequiredOptionValue(argv, index, "--exclude"));
            return index + 1;
        }

        if ("--verbose".equals(arg)) {
            state.verbose = true;
            return index;
        }

        if (arg.startsWith("--")) {
            throw new IllegalArgumentException("Unknown argument: " + arg);
        }

        state.positional.add(arg);
        return index;
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

    private static void applyPositionalDirectories(ParseState state) {
        if (state.inputDirectory == null && !state.positional.isEmpty()) {
            state.inputDirectory = state.positional.get(0);
        }

        if (state.outputDirectory == null && state.positional.size() > 1) {
            state.outputDirectory = state.positional.get(1);
        }

        if (state.positional.size() > 2) {
            throw new IllegalArgumentException("Unexpected positional argument: " + state.positional.get(2));
        }
    }

    private static final class ParseState {
        private String inputDirectory;
        private String outputDirectory;
        private int maxChars;
        private int maxInputFileBytes;
        private EncodingOptions encoding;
        private List<String> includePatterns;
        private List<String> excludePatterns;
        private boolean verbose;
        private List<String> positional;
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
