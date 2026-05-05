package jp.igapyon.mikutextbundle.cli;

import java.io.PrintStream;

import jp.igapyon.mikutextbundle.core.MikuTextBundle;

/**
 * Command line entrypoint for miku-text-bundle-java.
 */
public final class MikuTextBundleCli {
    private MikuTextBundleCli() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || isHelp(args[0])) {
            printUsage(out);
            return 0;
        }

        if ("--version".equals(args[0])) {
            out.println(MikuTextBundle.productName() + " " + MikuTextBundle.VERSION);
            return 0;
        }

        err.println("Unknown option or command: " + args[0]);
        err.println("Run with --help for usage.");
        return 2;
    }

    private static boolean isHelp(String value) {
        return "--help".equals(value) || "-h".equals(value);
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: miku-text-bundle-java [--help] [--version]");
        out.println();
        out.println("Options:");
        out.println("  -h, --help     Show this help.");
        out.println("      --version  Show the version.");
    }
}
