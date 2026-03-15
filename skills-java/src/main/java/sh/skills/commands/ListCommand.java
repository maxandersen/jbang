package sh.skills.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import sh.skills.agents.AgentRegistry;
import sh.skills.model.AgentConfig;
import sh.skills.model.Skill;
import sh.skills.providers.SkillDiscovery;
import sh.skills.util.Console;
import sh.skills.util.FrontmatterParser;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * Implements `skills list` (alias: `skills ls`).
 * Lists all installed skills for each agent.
 * Mirrors src/list.ts from the TypeScript source.
 */
@Command(
    name = "list",
    aliases = {"ls"},
    description = "List installed skills for all agents",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Callable<Integer> {

    @Option(names = {"-g", "--global"}, description = "List globally installed skills instead of project-level")
    private boolean global;

    @Option(names = {"--json"}, description = "Output as JSON")
    private boolean json;

    @Option(names = {"-a", "--agent"}, description = "Filter to a specific agent")
    private String agentFilter;

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
        List<AgentConfig> agents = AgentRegistry.getAgents();
        if (agentFilter != null) {
            agents = new ArrayList<>();
            AgentRegistry.findByName(agentFilter).ifPresent(agents::add);
            if (agents.isEmpty()) {
                Console.error("Unknown agent: " + agentFilter);
                return 1;
            }
        }

        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        String home = System.getProperty("user.home");
        String projectDir = System.getProperty("user.dir");

        for (AgentConfig agent : agents) {
            Path skillsDir;
            if (global) {
                skillsDir = Paths.get(home, agent.getGlobalSkillsDir() != null
                    ? agent.getGlobalSkillsDir() : agent.getSkillsDir());
            } else {
                skillsDir = Paths.get(projectDir, agent.getSkillsDir());
            }

            if (!Files.isDirectory(skillsDir)) continue;

            List<Skill> skills = discoverInstalledSkills(skillsDir);
            if (skills.isEmpty()) continue;

            List<Map<String, String>> skillList = new ArrayList<>();
            for (Skill skill : skills) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("name", skill.getName());
                entry.put("description", skill.getDescription());
                skillList.add(entry);
            }
            result.put(agent.getName(), skillList);
        }

        if (json) {
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            System.out.println(mapper.writeValueAsString(result));
            return 0;
        }

        if (result.isEmpty()) {
            Console.log("No skills installed." + (global ? "" : " (project-level)"));
            Console.log("Run " + Console.cyan("skills add <source>") + " to install skills.");
            return 0;
        }

        for (Map.Entry<String, List<Map<String, String>>> entry : result.entrySet()) {
            String agentName = entry.getKey();
            AgentConfig agent = AgentRegistry.findByName(agentName).orElse(null);
            String displayName = agent != null ? agent.getDisplayName() : agentName;

            Console.log("\n" + Console.bold(displayName) + " (" + entry.getValue().size() + " skill(s)):");
            for (Map<String, String> skill : entry.getValue()) {
                Console.log("  " + Console.green("•") + " " + Console.bold(skill.get("name"))
                    + " - " + Console.dim(skill.get("description")));
            }
        }
        return 0;
    }

    private List<Skill> discoverInstalledSkills(Path skillsDir) {
        List<Skill> skills = new ArrayList<>();
        if (!Files.isDirectory(skillsDir)) return skills;

        try (Stream<Path> entries = Files.list(skillsDir)) {
            entries.filter(Files::isDirectory).forEach(subdir -> {
                Path skillFile = subdir.resolve("SKILL.md");
                if (Files.exists(skillFile)) {
                    try {
                        String content = Files.readString(skillFile);
                        Skill skill = FrontmatterParser.parse(content, skillFile);
                        if (skill != null) skills.add(skill);
                    } catch (IOException ignored) {}
                }
            });
        } catch (IOException ignored) {}

        // Also check for SKILL.md directly in the skills dir
        Path rootSkillFile = skillsDir.resolve("SKILL.md");
        if (Files.exists(rootSkillFile)) {
            try {
                String content = Files.readString(rootSkillFile);
                Skill skill = FrontmatterParser.parse(content, rootSkillFile);
                if (skill != null) skills.add(skill);
            } catch (IOException ignored) {}
        }

        skills.sort(Comparator.comparing(Skill::getName));
        return skills;
    }
}
