package sh.skills.model;

import java.nio.file.Path;

/**
 * Represents a parsed SKILL.md file.
 * Mirrors the TypeScript Skill interface from src/types.ts.
 */
public class Skill {
    private final String name;
    private final String description;
    private final String content;
    private final Path filePath;
    private final boolean internal;

    public Skill(String name, String description, String content, Path filePath, boolean internal) {
        this.name = name;
        this.description = description;
        this.content = content;
        this.filePath = filePath;
        this.internal = internal;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getContent() { return content; }
    public Path getFilePath() { return filePath; }
    public boolean isInternal() { return internal; }

    @Override
    public String toString() {
        return "Skill{name='" + name + "', description='" + description + "'}";
    }
}
