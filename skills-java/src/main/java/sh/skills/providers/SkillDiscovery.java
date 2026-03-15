package sh.skills.providers;

import sh.skills.model.PluginManifest;
import sh.skills.model.Skill;
import sh.skills.util.FrontmatterParser;
import sh.skills.util.PathUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Discovers SKILL.md files within a cloned/local directory.
 * Mirrors the skill discovery logic from src/skills.ts.
 *
 * Search priority:
 * 1. Root SKILL.md
 * 2. skills/, skills/.curated/, skills/.experimental/, skills/.system/
 * 3. Plugin manifest paths (.claude-plugin/marketplace.json or plugin.json)
 * 4. Recursive fallback search (max depth 5)
 */
public class SkillDiscovery {

    private static final String SKILL_FILE = "SKILL.md";
    private static final int MAX_DEPTH = 5;

    private static final List<String> PRIORITY_DIRS = Arrays.asList(
        "skills",
        "skills/.curated",
        "skills/.experimental",
        "skills/.system"
    );

    private static final List<String> PLUGIN_MANIFEST_PATHS = Arrays.asList(
        ".claude-plugin/marketplace.json",
        ".claude-plugin/plugin.json"
    );

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Discover all skills in the given root directory.
     *
     * @param root      Base directory to search
     * @param skillName Optional skill name filter (null = find all)
     * @param includeInternal Include skills marked as internal
     */
    public List<Skill> discover(Path root, String skillName, boolean includeInternal) {
        List<Skill> skills = new ArrayList<>();

        // 1. Root SKILL.md
        tryAddSkill(root.resolve(SKILL_FILE), skills, includeInternal);

        // 2. Priority directories
        for (String dir : PRIORITY_DIRS) {
            Path skillsDir = root.resolve(dir);
            if (Files.isDirectory(skillsDir)) {
                try (Stream<Path> entries = Files.list(skillsDir)) {
                    entries.filter(Files::isDirectory)
                           .forEach(subdir -> tryAddSkill(subdir.resolve(SKILL_FILE), skills, includeInternal));
                } catch (IOException ignored) {}
                // Also check for SKILL.md directly in the priority dir
                tryAddSkill(skillsDir.resolve(SKILL_FILE), skills, includeInternal);
            }
        }

        // 3. Plugin manifest
        for (String manifestPath : PLUGIN_MANIFEST_PATHS) {
            Path manifestFile = root.resolve(manifestPath);
            if (Files.exists(manifestFile)) {
                discoverFromManifest(root, manifestFile, skills, includeInternal);
                break;
            }
        }

        // 4. Recursive fallback if nothing found yet
        if (skills.isEmpty()) {
            discoverRecursive(root, root, 0, skills, includeInternal);
        }

        // Apply name filter
        if (skillName != null && !skillName.isEmpty()) {
            skills.removeIf(s -> !matchesName(s.getName(), skillName));
        }

        return skills;
    }

    private void tryAddSkill(Path skillFile, List<Skill> skills, boolean includeInternal) {
        if (!Files.exists(skillFile)) return;
        try {
            String content = Files.readString(skillFile);
            Skill skill = FrontmatterParser.parse(content, skillFile);
            if (skill != null && (includeInternal || !skill.isInternal())) {
                // Avoid duplicates by name
                boolean exists = skills.stream().anyMatch(s -> s.getName().equals(skill.getName()));
                if (!exists) {
                    skills.add(skill);
                }
            }
        } catch (IOException ignored) {}
    }

    private void discoverFromManifest(Path root, Path manifestFile, List<Skill> skills, boolean includeInternal) {
        try {
            PluginManifest manifest = mapper.readValue(manifestFile.toFile(), PluginManifest.class);
            if (manifest.getPlugins() == null) return;

            String pluginRoot = manifest.getMetadata() != null && manifest.getMetadata().getPluginRoot() != null
                ? manifest.getMetadata().getPluginRoot()
                : ".";

            for (PluginManifest.Plugin plugin : manifest.getPlugins()) {
                if (plugin.getSkills() == null) continue;
                for (String skillPath : plugin.getSkills()) {
                    Path resolvedSkillDir = root.resolve(pluginRoot).resolve(skillPath).normalize();
                    if (!PathUtils.isSubpathSafe(root, resolvedSkillDir)) continue;
                    tryAddSkill(resolvedSkillDir.resolve(SKILL_FILE), skills, includeInternal);
                }
            }
        } catch (IOException ignored) {}
    }

    private void discoverRecursive(Path root, Path current, int depth, List<Skill> skills, boolean includeInternal) {
        if (depth > MAX_DEPTH) return;
        if (!Files.isDirectory(current)) return;

        tryAddSkill(current.resolve(SKILL_FILE), skills, includeInternal);

        try (Stream<Path> entries = Files.list(current)) {
            entries.filter(Files::isDirectory)
                   .filter(p -> !p.getFileName().toString().startsWith(".") || depth == 0)
                   .forEach(subdir -> discoverRecursive(root, subdir, depth + 1, skills, includeInternal));
        } catch (IOException ignored) {}
    }

    /**
     * Match a skill name against a filter, case-insensitively.
     * Supports both kebab-case and space-separated names.
     */
    public static boolean matchesName(String skillName, String filter) {
        if (skillName == null || filter == null) return false;
        String normalizedSkill = normalize(skillName);
        String normalizedFilter = normalize(filter);
        return normalizedSkill.equals(normalizedFilter)
            || normalizedSkill.contains(normalizedFilter);
    }

    private static String normalize(String name) {
        return name.toLowerCase()
                   .replace(" ", "-")
                   .replace("_", "-")
                   .trim();
    }
}
