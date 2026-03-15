package sh.skills.util;

import org.yaml.snakeyaml.Yaml;
import sh.skills.model.Skill;

import java.nio.file.Path;
import java.util.Map;

/**
 * Parses SKILL.md files with YAML frontmatter.
 * Format:
 * ---
 * name: skill-name
 * description: A description
 * ---
 * # Markdown content...
 */
public class FrontmatterParser {

    private static final String DELIMITER = "---";

    /**
     * Parse a SKILL.md file content into a Skill object.
     * Returns null if the file does not have valid frontmatter or required fields.
     */
    public static Skill parse(String content, Path filePath) {
        if (content == null || !content.startsWith(DELIMITER)) {
            return null;
        }

        // Find the closing ---
        int start = DELIMITER.length();
        // Skip optional newline after opening ---
        if (start < content.length() && content.charAt(start) == '\n') start++;
        else if (start < content.length() && content.charAt(start) == '\r') {
            start++;
            if (start < content.length() && content.charAt(start) == '\n') start++;
        }

        int end = content.indexOf('\n' + DELIMITER, start);
        if (end == -1) {
            // Try without leading newline (e.g. file starts immediately with ---)
            end = content.indexOf(DELIMITER, start);
            if (end == -1) return null;
        }

        String yamlSection = content.substring(start, end);
        String markdownContent = content.substring(end + DELIMITER.length() + 1).stripLeading();

        // Parse YAML
        Yaml yaml = new Yaml();
        Object parsed;
        try {
            parsed = yaml.load(yamlSection);
        } catch (Exception e) {
            return null;
        }

        if (!(parsed instanceof Map)) return null;

        @SuppressWarnings("unchecked")
        Map<String, Object> frontmatter = (Map<String, Object>) parsed;

        Object nameObj = frontmatter.get("name");
        Object descObj = frontmatter.get("description");

        if (nameObj == null || descObj == null) return null;

        String name = nameObj.toString().trim();
        String description = descObj.toString().trim();

        if (name.isEmpty() || description.isEmpty()) return null;

        // Check for metadata.internal
        boolean internal = false;
        Object metaObj = frontmatter.get("metadata");
        if (metaObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) metaObj;
            Object internalObj = meta.get("internal");
            if (internalObj instanceof Boolean) {
                internal = (Boolean) internalObj;
            }
        }

        return new Skill(name, description, markdownContent, filePath, internal);
    }
}
