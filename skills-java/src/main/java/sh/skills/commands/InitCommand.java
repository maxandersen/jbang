package sh.skills.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import sh.skills.util.Console;
import sh.skills.util.PathUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Callable;

/**
 * Implements `skills init [name]`.
 * Creates a new SKILL.md template in the current directory.
 * Mirrors src/init.ts from the TypeScript source.
 */
@Command(
    name = "init",
    description = "Create a new SKILL.md template",
    mixinStandardHelpOptions = true
)
public class InitCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", description = "Skill name (optional)")
    private String name;

    @Option(names = {"-d", "--dir"}, description = "Directory to create the skill in (default: ./skills/<name>)")
    private String dir;

    @Option(names = {"-f", "--force"}, description = "Overwrite existing SKILL.md")
    private boolean force;

    @Override
    public Integer call() {
        try {
            return execute();
        } catch (Exception e) {
            Console.error(e.getMessage());
            return 1;
        }
    }

    private int execute() throws IOException {
        // Prompt for name if not provided
        if (name == null || name.isEmpty()) {
            Console.print("Skill name (kebab-case): ");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            name = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        }

        if (name.isEmpty()) {
            Console.error("Skill name is required.");
            return 1;
        }

        String sanitized = PathUtils.sanitizeName(name);

        // Determine output directory
        Path skillDir;
        if (dir != null) {
            skillDir = Paths.get(dir);
        } else {
            skillDir = Paths.get(System.getProperty("user.dir"), "skills", sanitized);
        }

        Path skillFile = skillDir.resolve("SKILL.md");

        if (Files.exists(skillFile) && !force) {
            Console.error("SKILL.md already exists at " + skillFile);
            Console.log("Use --force to overwrite.");
            return 1;
        }

        Files.createDirectories(skillDir);

        String template = "---\n" +
            "name: " + sanitized + "\n" +
            "description: A brief description of what this skill does\n" +
            "---\n" +
            "\n" +
            "# " + toTitleCase(sanitized) + "\n" +
            "\n" +
            "## Overview\n" +
            "\n" +
            "Describe what this skill enables the agent to do.\n" +
            "\n" +
            "## Instructions\n" +
            "\n" +
            "1. Step one\n" +
            "2. Step two\n" +
            "3. Step three\n" +
            "\n" +
            "## Examples\n" +
            "\n" +
            "Provide example usage or scenarios.\n";

        Files.writeString(skillFile, template);

        Console.success("Created " + Console.bold(skillFile.toString()));
        Console.log("\nEdit the file to add your skill instructions.");
        return 0;
    }

    private String toTitleCase(String name) {
        String[] parts = name.split("[-_]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
