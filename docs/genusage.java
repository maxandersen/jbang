//DEPS info.picocli:picocli:4.7.7
///COMPILE_OPTIONS -proc:none
//JAVA 17+

import dev.jbang.cli.Completion;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

/**
 * Generates a KDL usage spec file (https://usage.jdx.dev/) for the jbang CLI.
 * <p>
 * This script is analogous to {@code genadoc.java} but produces a KDL spec
 * instead of AsciiDoc man pages. The generated file can be used with the
 * {@code usage} CLI tool to generate shell completions, man pages, and
 * markdown documentation.
 * </p>
 * <p>
 * Run via Gradle's {@code cliusage} task, or directly:
 * </p>
 * <pre>
 *   jbang --cp build/install/jbang/bin/jbang.jar docs/genusage.java \
 *       --outfile jbang.usage.kdl --force dev.jbang.cli.JBang
 * </pre>
 *
 * <p>
 * Alternatively, from a running jbang instance:
 * </p>
 * <pre>
 *   jbang completion --shell usage &gt; jbang.usage.kdl
 * </pre>
 */
@Command(name = "gen-usage", mixinStandardHelpOptions = true, sortOptions = false,
        description = { "Generates a KDL usage spec file for the jbang CLI.",
                "See https://usage.jdx.dev/ for details on the spec format." })
public class genusage implements Callable<Integer> {

    @Option(names = { "-o", "--outfile" }, defaultValue = "jbang.usage.kdl", paramLabel = "<outfile>",
            description = "Output file to write the generated KDL spec to. Defaults to ${DEFAULT-VALUE}.")
    File outfile;

    @Option(names = { "-f", "--force" }, negatable = true,
            description = "Overwrite existing output file. Default: ${DEFAULT-VALUE}.")
    boolean force;

    @Parameters(arity = "1", description = "Fully-qualified class name of the root @Command class to generate spec for.",
            paramLabel = "<class>")
    String className;

    public Integer call() throws Exception {
        if (outfile.exists() && !force) {
            System.err.println("gen-usage: " + outfile + " already exists. Use --force to overwrite.");
            return 4;
        }

        CommandLine commandLine = buildCommandLine(className);
        String spec = Completion.usageKdl(commandLine.getCommandSpec().name(), commandLine);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outfile))) {
            writer.print(spec);
        }
        System.out.println("gen-usage: wrote " + outfile.getAbsolutePath());
        return 0;
    }

    private static CommandLine buildCommandLine(String className) throws Exception {
        Class<?> cls = Class.forName(className);
        Object instance = cls.getDeclaredConstructor().newInstance();
        return new CommandLine(instance);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new genusage()).execute(args);
        System.exit(exitCode);
    }
}
