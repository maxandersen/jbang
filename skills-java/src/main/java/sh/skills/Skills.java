///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS sh.skills:skills-java:LATEST

package sh.skills;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import sh.skills.commands.*;
import sh.skills.util.Console;

import java.util.concurrent.Callable;

/**
 * Main entry point for the skills CLI.
 * Mirrors src/cli.ts from the TypeScript source (vercel-labs/skills).
 *
 * Usage via JBang:
 *   jbang skills@jbangdev/skills-java [command] [options]
 *
 * Or as a fat jar:
 *   java -jar skills-java.jar [command] [options]
 */
@Command(
    name = "skills",
    description = "The CLI for the open agent skills ecosystem",
    version = "1.0.0",
    mixinStandardHelpOptions = true,
    subcommands = {
        AddCommand.class,
        ListCommand.class,
        RemoveCommand.class,
        CheckCommand.class,
        UpdateCommand.class,
        InitCommand.class,
        FindCommand.class,
        CommandLine.HelpCommand.class
    },
    footer = {
        "",
        "Examples:",
        "  skills add vercel-labs/agent-skills",
        "  skills add https://github.com/org/my-skills --agent claude-code",
        "  skills list",
        "  skills find web design",
        "  skills check",
        "  skills update",
        "  skills remove my-skill",
        "  skills init my-new-skill",
        "",
        "Documentation: https://github.com/jbangdev/skills-java",
        "Original project: https://github.com/vercel-labs/skills"
    }
)
public class Skills implements Callable<Integer> {

    @Option(names = {"--version"}, versionHelp = true, description = "Print version information")
    private boolean versionRequested;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Skills())
            .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                Console.error(ex.getMessage());
                return 1;
            })
            .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // No subcommand: show help
        new CommandLine(this).usage(System.out);
        return 0;
    }
}
