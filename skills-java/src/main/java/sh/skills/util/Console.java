package sh.skills.util;

/**
 * Terminal output utilities with ANSI color support.
 * Ports picocolors functionality from the TypeScript source.
 * Automatically detects TTY and disables colors in non-interactive/CI environments.
 */
public class Console {

    private static final boolean COLORS_ENABLED = detectColors();

    private static boolean detectColors() {
        // Disable colors if NO_COLOR env var is set (https://no-color.org/)
        if (System.getenv("NO_COLOR") != null) return false;
        // Disable colors in CI by default unless FORCE_COLOR is set
        if (System.getenv("FORCE_COLOR") != null) return true;
        if (System.getenv("CI") != null) return false;
        // Check if stdout is a TTY
        return System.console() != null;
    }

    // ANSI codes
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String DIM    = "\u001B[2m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE   = "\u001B[34m";
    private static final String CYAN   = "\u001B[36m";
    private static final String RED    = "\u001B[31m";
    private static final String GRAY   = "\u001B[90m";
    private static final String WHITE  = "\u001B[37m";

    private static String apply(String code, String text) {
        if (!COLORS_ENABLED) return text;
        return code + text + RESET;
    }

    public static String green(String text)  { return apply(GREEN, text); }
    public static String yellow(String text) { return apply(YELLOW, text); }
    public static String blue(String text)   { return apply(BLUE, text); }
    public static String cyan(String text)   { return apply(CYAN, text); }
    public static String red(String text)    { return apply(RED, text); }
    public static String gray(String text)   { return apply(GRAY, text); }
    public static String bold(String text)   { return apply(BOLD, text); }
    public static String dim(String text)    { return apply(DIM, text); }

    // Semantic wrappers
    public static void success(String msg) {
        System.out.println(green("✓") + " " + msg);
    }

    public static void info(String msg) {
        System.out.println(blue("ℹ") + " " + msg);
    }

    public static void warn(String msg) {
        System.out.println(yellow("⚠") + " " + msg);
    }

    public static void error(String msg) {
        System.err.println(red("✗") + " " + msg);
    }

    public static void log(String msg) {
        System.out.println(msg);
    }

    public static void step(String msg) {
        System.out.println(cyan("◆") + " " + msg);
    }

    public static void print(String msg) {
        System.out.print(msg);
    }
}
