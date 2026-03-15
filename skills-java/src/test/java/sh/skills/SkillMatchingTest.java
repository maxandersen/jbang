package sh.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sh.skills.providers.SkillDiscovery;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for skill name matching logic.
 * Driven by the shared test fixture: test-fixtures/skill-matching-cases.json
 */
@DisplayName("Skill Matching")
class SkillMatchingTest {

    record TestCase(String description, List<String> skills, String filter, List<String> expected) {}

    static Stream<TestCase> loadCases() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = SkillMatchingTest.class.getResourceAsStream("/test-fixtures/skill-matching-cases.json");
        if (is == null) {
            // Try loading from file system for IDE usage
            is = java.nio.file.Files.newInputStream(
                java.nio.file.Paths.get("test-fixtures/skill-matching-cases.json"));
        }
        JsonNode root = mapper.readTree(is);
        List<TestCase> cases = new ArrayList<>();
        for (JsonNode c : root.get("cases")) {
            List<String> skills = new ArrayList<>();
            for (JsonNode s : c.get("skills")) skills.add(s.asText());
            List<String> expected = new ArrayList<>();
            for (JsonNode e : c.get("expected")) expected.add(e.asText());
            cases.add(new TestCase(
                c.get("description").asText(),
                skills,
                c.get("filter").asText(),
                expected
            ));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadCases")
    @DisplayName("Matching case")
    void testMatching(TestCase tc) {
        if (tc.filter().isEmpty()) {
            // Empty filter: all skills match
            assertThat(tc.skills()).containsExactlyInAnyOrderElementsOf(tc.expected());
            return;
        }

        List<String> matched = tc.skills().stream()
            .filter(name -> SkillDiscovery.matchesName(name, tc.filter()))
            .toList();

        assertThat(matched)
            .as(tc.description())
            .containsExactlyInAnyOrderElementsOf(tc.expected());
    }
}
