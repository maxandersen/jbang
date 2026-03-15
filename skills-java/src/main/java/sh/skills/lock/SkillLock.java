package sh.skills.lock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import sh.skills.model.SkillLockEntry;
import sh.skills.util.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the global ~/.skill-lock.json file.
 * Tracks installed skills and their versions for update detection.
 * Mirrors skill-lock.ts from the TypeScript source.
 */
public class SkillLock {

    private final Path lockFile;
    private final ObjectMapper mapper;

    public SkillLock() {
        this(PathUtils.globalSkillLockPath());
    }

    public SkillLock(Path lockFile) {
        this.lockFile = lockFile;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Read all lock entries. Returns empty map if file doesn't exist.
     * Key is the skill name + agent, value is the lock entry.
     */
    public Map<String, SkillLockEntry> readAll() {
        if (!Files.exists(lockFile)) return new HashMap<>();
        try {
            return mapper.readValue(lockFile.toFile(),
                new TypeReference<Map<String, SkillLockEntry>>() {});
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    /**
     * Write the lock entry for a skill.
     * Key format: "agent:skillName"
     */
    public void write(String agent, String skillName, SkillLockEntry entry) throws IOException {
        Map<String, SkillLockEntry> all = readAll();
        all.put(agent + ":" + skillName, entry);
        save(all);
    }

    /**
     * Remove a lock entry.
     */
    public void remove(String agent, String skillName) throws IOException {
        Map<String, SkillLockEntry> all = readAll();
        all.remove(agent + ":" + skillName);
        save(all);
    }

    /**
     * Get a specific lock entry.
     */
    public SkillLockEntry get(String agent, String skillName) {
        return readAll().get(agent + ":" + skillName);
    }

    private void save(Map<String, SkillLockEntry> data) throws IOException {
        Files.createDirectories(lockFile.getParent());
        mapper.writeValue(lockFile.toFile(), data);
    }
}
