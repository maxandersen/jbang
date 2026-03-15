package sh.skills.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for a supported coding agent.
 * Mirrors the AgentConfig interface from src/agents.ts.
 */
public class AgentConfig {
    private final String name;
    private final String displayName;
    /** Relative path for project-level skills installation (e.g. ".claude/skills") */
    private final String skillsDir;
    /** Relative path from home dir for global skills (null if not supported) */
    private final String globalSkillsDir;
    /** Path used to detect if the agent is installed */
    private final String detectPath;

    public AgentConfig(String name, String displayName, String skillsDir,
                       String globalSkillsDir, String detectPath) {
        this.name = name;
        this.displayName = displayName;
        this.skillsDir = skillsDir;
        this.globalSkillsDir = globalSkillsDir;
        this.detectPath = detectPath;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getSkillsDir() { return skillsDir; }
    public String getGlobalSkillsDir() { return globalSkillsDir; }

    /**
     * Returns true if this agent appears to be installed on this machine.
     */
    public boolean isInstalled() {
        if (detectPath == null) return false;
        String home = System.getProperty("user.home");
        Path p = Paths.get(home, detectPath);
        return Files.exists(p);
    }

    @Override
    public String toString() {
        return displayName + " (" + name + ")";
    }
}
