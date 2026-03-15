package sh.skills.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import sh.skills.agents.AgentRegistry;
import sh.skills.lock.LocalLock;
import sh.skills.lock.SkillLock;
import sh.skills.model.AgentConfig;
import sh.skills.util.Console;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Implements `skills remove` (alias: `skills rm`).
 * Removes installed skills from agents.
 * Mirrors src/remove.ts from the TypeScript source.
 */
@Command(
    name = "remove",
    aliases = {"rm"},
    description = "Remove installed skills",
    mixinStandardHelpOptions = true
)
public class RemoveCommand implements Callable<Integer> {

    @Parameters(arity = "0..*", description = "Skill name(s) to remove")
    private List<String> skillNames = new ArrayList<>();

    @Option(names = {"-a", "--agent"}, description = "Remove from specific agent(s)")
    private List<String> agents = new ArrayList<>();

    @Option(names = {"-g", "--global"}, description = "Remove globally installed skills")
    private boolean global;

    @Option(names = {"-y", "--yes"}, description = "Skip confirmation prompt")
    private boolean yes;

    @Option(names = {"--all"}, description = "Remove all installed skills")
    private boolean all;

    @Override
    public Integer call() {
        try {
            return execute();
        } catch (Exception e) {
            Console.error(e.getMessage());
            return 1;
        }
    }

    private int execute() throws Exception {
        List<AgentConfig> targetAgents = resolveAgents();
        if (targetAgents.isEmpty()) {
            Console.error("No agents selected.");
            return 1;
        }

        String home = System.getProperty("user.home");
        String projectDir = System.getProperty("user.dir");
        SkillLock globalLock = new SkillLock();
        LocalLock localLock = new LocalLock(Paths.get(projectDir));

        int removed = 0;
        for (AgentConfig agent : targetAgents) {
            Path skillsDir = global
                ? Paths.get(home, agent.getGlobalSkillsDir() != null
                    ? agent.getGlobalSkillsDir() : agent.getSkillsDir())
                : Paths.get(projectDir, agent.getSkillsDir());

            if (!Files.isDirectory(skillsDir)) continue;

            List<String> toRemove = skillNames.isEmpty() || all
                ? listInstalledSkillNames(skillsDir)
                : new ArrayList<>(skillNames);

            for (String skillName : toRemove) {
                Path skillDir = skillsDir.resolve(skillName);
                if (!Files.exists(skillDir)) {
                    Console.warn("Skill '" + skillName + "' not found in " + agent.getDisplayName());
                    continue;
                }

                if (!yes && !all) {
                    Console.print("Remove " + Console.bold(skillName) + " from " + agent.getDisplayName() + "? [y/N] ");
                    Scanner scanner = new Scanner(System.in);
                    String answer = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
                    if (!answer.equalsIgnoreCase("y") && !answer.equalsIgnoreCase("yes")) {
                        continue;
                    }
                }

                try {
                    deleteRecursive(skillDir);
                    // Update lock files
                    if (global) {
                        globalLock.remove(agent.getName(), skillName);
                    } else {
                        localLock.remove(agent.getName(), skillName);
                    }
                    Console.success("Removed " + Console.bold(skillName)
                        + " from " + agent.getDisplayName());
                    removed++;
                } catch (IOException e) {
                    Console.error("Failed to remove " + skillName + ": " + e.getMessage());
                }
            }
        }

        if (removed == 0) {
            Console.log("No skills removed.");
        } else {
            Console.log("\nRemoved " + removed + " skill(s).");
        }
        return 0;
    }

    private List<String> listInstalledSkillNames(Path skillsDir) throws IOException {
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(skillsDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    names.add(entry.getFileName().toString());
                }
            }
        }
        return names;
    }

    private List<AgentConfig> resolveAgents() {
        if (!agents.isEmpty()) {
            List<AgentConfig> result = new ArrayList<>();
            for (String name : agents) {
                AgentRegistry.findByName(name).ifPresent(result::add);
            }
            return result;
        }
        List<AgentConfig> installed = AgentRegistry.getInstalledAgents();
        return installed.isEmpty() ? AgentRegistry.getAgents() : installed;
    }

    private void deleteRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
