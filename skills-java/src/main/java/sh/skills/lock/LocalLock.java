package sh.skills.lock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import sh.skills.model.SkillLockEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the project-level local-lock.json file.
 * Tracks project-level skill installations.
 * Mirrors local-lock.ts from the TypeScript source.
 */
public class LocalLock {

    private final Path lockFile;
    private final ObjectMapper mapper;

    public LocalLock(Path projectDir) {
        this.lockFile = projectDir.resolve("local-lock.json");
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Map<String, SkillLockEntry> readAll() {
        if (!Files.exists(lockFile)) return new HashMap<>();
        try {
            return mapper.readValue(lockFile.toFile(),
                new TypeReference<Map<String, SkillLockEntry>>() {});
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public void write(String agent, String skillName, SkillLockEntry entry) throws IOException {
        Map<String, SkillLockEntry> all = readAll();
        all.put(agent + ":" + skillName, entry);
        save(all);
    }

    public void remove(String agent, String skillName) throws IOException {
        Map<String, SkillLockEntry> all = readAll();
        all.remove(agent + ":" + skillName);
        save(all);
    }

    public SkillLockEntry get(String agent, String skillName) {
        return readAll().get(agent + ":" + skillName);
    }

    public Path getLockFile() { return lockFile; }

    private void save(Map<String, SkillLockEntry> data) throws IOException {
        Files.createDirectories(lockFile.getParent());
        mapper.writeValue(lockFile.toFile(), data);
    }
}
