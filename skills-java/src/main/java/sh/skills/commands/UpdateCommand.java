package sh.skills.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import sh.skills.lock.LocalLock;
import sh.skills.lock.SkillLock;
import sh.skills.model.Skill;
import sh.skills.model.SkillLockEntry;
import sh.skills.providers.HostProvider;
import sh.skills.providers.ProviderRegistry;
import sh.skills.util.Console;
import sh.skills.util.PathUtils;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Implements `skills update`.
 * Updates installed skills to their latest versions.
 * Mirrors src/update.ts from the TypeScript source.
 */
@Command(
    name = "update",
    description = "Update installed skills to their latest versions",
    mixinStandardHelpOptions = true
)
public class UpdateCommand implements Callable<Integer> {

    @Parameters(arity = "0..*", description = "Skill name(s) to update (default: all)")
    private List<String> skillNames = new ArrayList<>();

    @Option(names = {"-g", "--global"}, description = "Update globally installed skills")
    private boolean global;

    @Option(names = {"-y", "--yes"}, description = "Skip confirmation prompts")
    private boolean yes;

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
        SkillLock globalLock = new SkillLock();
        LocalLock localLock = new LocalLock(Paths.get(System.getProperty("user.dir")));
        Map<String, SkillLockEntry> entries = global ? globalLock.readAll() : localLock.readAll();

        if (entries.isEmpty()) {
            Console.log("No skills to update.");
            return 0;
        }

        // Filter to requested skills
        if (!skillNames.isEmpty()) {
            entries.entrySet().removeIf(e -> {
                String name = e.getKey().contains(":") ? e.getKey().substring(e.getKey().indexOf(':') + 1) : e.getKey();
                return skillNames.stream().noneMatch(n -> n.equalsIgnoreCase(name));
            });
        }

        Console.step("Updating " + entries.size() + " skill(s)...");

        int updated = 0;
        String home = System.getProperty("user.home");
        String projectDir = System.getProperty("user.dir");

        for (Map.Entry<String, SkillLockEntry> entry : entries.entrySet()) {
            String key = entry.getKey();
            SkillLockEntry lock = entry.getValue();

            String agentName = key.contains(":") ? key.substring(0, key.indexOf(':')) : key;
            String skillName = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;

            if (lock.getSource() == null) continue;

            HostProvider provider = ProviderRegistry.findProvider(lock.getSource());
            if (provider == null) continue;

            try {
                Path tempDir = Files.createTempDirectory("skills-update-");
                List<Skill> skills = provider.fetchSkills(lock.getSource(), tempDir);

                Skill matchedSkill = skills.stream()
                    .filter(s -> s.getName().equalsIgnoreCase(skillName))
                    .findFirst()
                    .orElse(null);

                if (matchedSkill == null) {
                    Console.warn("Skill '" + skillName + "' not found in source, skipping.");
                    continue;
                }

                // Find agent config
                sh.skills.agents.AgentRegistry.findByName(agentName).ifPresent(agent -> {
                    try {
                        Path skillsDir = global
                            ? Paths.get(home, agent.getGlobalSkillsDir() != null
                                ? agent.getGlobalSkillsDir() : agent.getSkillsDir())
                            : Paths.get(projectDir, agent.getSkillsDir());

                        Path skillDir = skillsDir.resolve(PathUtils.sanitizeName(skillName));
                        Path skillFile = skillDir.resolve("SKILL.md");

                        Files.createDirectories(skillDir);
                        String content = "---\nname: " + matchedSkill.getName()
                            + "\ndescription: " + matchedSkill.getDescription()
                            + "\n---\n" + (matchedSkill.getContent() != null ? matchedSkill.getContent() : "");
                        Files.writeString(skillFile, content);

                        // Update lock entry
                        lock.setUpdatedAt(java.time.Instant.now().toString());
                        try {
                            String latestHash = provider.getLatestHash(lock.getSource(), lock.getSkillPath());
                            if (latestHash != null) lock.setSkillFolderHash(latestHash);
                        } catch (Exception ignored) {}

                        if (global) {
                            globalLock.write(agentName, skillName, lock);
                        } else {
                            localLock.write(agentName, skillName, lock);
                        }
                    } catch (Exception e) {
                        Console.error("Failed to update " + skillName + ": " + e.getMessage());
                    }
                });

                Console.success("Updated " + Console.bold(skillName) + " (" + agentName + ")");
                updated++;
                GitUtils.deleteTempDir(tempDir.toFile());
            } catch (Exception e) {
                Console.error("Failed to update " + skillName + ": " + e.getMessage());
            }
        }

        if (updated == 0) {
            Console.log("No skills were updated.");
        } else {
            Console.log("\nUpdated " + updated + " skill(s).");
        }
        return 0;
    }

    // Needed for lock access in lambda
    private SkillLock globalLock;
    private LocalLock localLock;

    // Inline helper ref
    private static class GitUtils {
        static void deleteTempDir(java.io.File dir) {
            sh.skills.util.GitUtils.deleteTempDir(dir);
        }
    }
}
