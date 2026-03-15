package sh.skills.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import sh.skills.agents.AgentRegistry;
import sh.skills.lock.LocalLock;
import sh.skills.lock.SkillLock;
import sh.skills.model.AgentConfig;
import sh.skills.model.SkillLockEntry;
import sh.skills.providers.GitHubProvider;
import sh.skills.providers.HostProvider;
import sh.skills.providers.ProviderRegistry;
import sh.skills.util.Console;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Implements `skills check`.
 * Checks for available updates to installed skills.
 * Mirrors src/check.ts from the TypeScript source.
 */
@Command(
    name = "check",
    description = "Check for available updates to installed skills",
    mixinStandardHelpOptions = true
)
public class CheckCommand implements Callable<Integer> {

    @Option(names = {"-g", "--global"}, description = "Check globally installed skills")
    private boolean global;

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
            Console.log("No installed skills found in lock file.");
            Console.log("Run " + Console.cyan("skills add <source>") + " to install skills.");
            return 0;
        }

        Console.step("Checking " + entries.size() + " skill(s) for updates...");

        int updatesAvailable = 0;
        int errors = 0;

        for (Map.Entry<String, SkillLockEntry> entry : entries.entrySet()) {
            String key = entry.getKey();
            SkillLockEntry lock = entry.getValue();

            String agentName = key.contains(":") ? key.substring(0, key.indexOf(':')) : key;
            String skillName = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;

            if (lock.getSource() == null || lock.getSkillFolderHash() == null) {
                continue;
            }

            try {
                HostProvider provider = ProviderRegistry.findProvider(lock.getSource());
                if (provider == null) continue;

                String latestHash = provider.getLatestHash(lock.getSource(), lock.getSkillPath());
                if (latestHash == null) continue;

                if (!latestHash.equals(lock.getSkillFolderHash())) {
                    Console.log(Console.yellow("↑") + " " + Console.bold(skillName)
                        + " (" + agentName + ") - update available");
                    updatesAvailable++;
                } else {
                    Console.log(Console.green("✓") + " " + skillName + " (" + agentName + ") - up to date");
                }
            } catch (Exception e) {
                Console.warn("Could not check " + skillName + ": " + e.getMessage());
                errors++;
            }
        }

        System.out.println();
        if (updatesAvailable > 0) {
            Console.log(Console.yellow(updatesAvailable + " update(s) available.") +
                " Run " + Console.cyan("skills update") + " to update.");
        } else if (errors == 0) {
            Console.log(Console.green("All skills are up to date."));
        }

        return 0;
    }
}
