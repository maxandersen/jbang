package sh.skills;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import sh.skills.model.Skill;
import sh.skills.util.FrontmatterParser;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the YAML frontmatter parser.
 */
@DisplayName("Frontmatter Parser")
class FrontmatterParserTest {

    @Test
    @DisplayName("Parse valid skill frontmatter")
    void parseValidSkill() {
        String content = """
            ---
            name: web-design
            description: A skill for web design
            ---
            # Web Design

            Content here.
            """;

        Skill skill = FrontmatterParser.parse(content, Path.of("SKILL.md"));

        assertThat(skill).isNotNull();
        assertThat(skill.getName()).isEqualTo("web-design");
        assertThat(skill.getDescription()).isEqualTo("A skill for web design");
        assertThat(skill.getContent()).contains("Content here.");
        assertThat(skill.isInternal()).isFalse();
    }

    @Test
    @DisplayName("Parse internal skill")
    void parseInternalSkill() {
        String content = """
            ---
            name: internal-skill
            description: An internal skill
            metadata:
              internal: true
            ---
            """;

        Skill skill = FrontmatterParser.parse(content, Path.of("SKILL.md"));

        assertThat(skill).isNotNull();
        assertThat(skill.isInternal()).isTrue();
    }

    @Test
    @DisplayName("Return null for missing name")
    void returnNullForMissingName() {
        String content = """
            ---
            description: Missing name field
            ---
            """;

        Skill skill = FrontmatterParser.parse(content, Path.of("SKILL.md"));
        assertThat(skill).isNull();
    }

    @Test
    @DisplayName("Return null for missing description")
    void returnNullForMissingDescription() {
        String content = """
            ---
            name: test-skill
            ---
            """;

        Skill skill = FrontmatterParser.parse(content, Path.of("SKILL.md"));
        assertThat(skill).isNull();
    }

    @Test
    @DisplayName("Return null for content without frontmatter")
    void returnNullForNoFrontmatter() {
        String content = "# Just a markdown file\nNo frontmatter here.";
        Skill skill = FrontmatterParser.parse(content, Path.of("SKILL.md"));
        assertThat(skill).isNull();
    }

    @Test
    @DisplayName("Return null for null content")
    void returnNullForNullContent() {
        Skill skill = FrontmatterParser.parse(null, Path.of("SKILL.md"));
        assertThat(skill).isNull();
    }

    @Test
    @DisplayName("Parse multiline description")
    void parseMultilineDescription() {
        String content = """
            ---
            name: complex-skill
            description: "A skill with a longer description that spans content"
            ---
            Content.
            """;

        Skill skill = FrontmatterParser.parse(content, Path.of("SKILL.md"));
        assertThat(skill).isNotNull();
        assertThat(skill.getDescription()).isNotEmpty();
    }
}
