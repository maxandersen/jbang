package sh.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sh.skills.util.PathUtils;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for path traversal prevention and name sanitization.
 * Driven by test-fixtures/subpath-traversal-cases.json
 */
@DisplayName("Subpath Traversal Security")
class SubpathTraversalTest {

    record PathCase(String description, String base, String subpath, boolean expectedSafe) {}
    record SanitizeCase(String description, String input, String expectedOutput) {}

    static Stream<PathCase> loadPathCases() throws Exception {
        return loadRoot().flatMap(root -> {
            List<PathCase> cases = new ArrayList<>();
            for (JsonNode c : root.get("cases")) {
                cases.add(new PathCase(
                    c.get("description").asText(),
                    c.get("base").asText(),
                    c.get("subpath").asText(),
                    c.get("expectedSafe").asBoolean()
                ));
            }
            return cases.stream();
        });
    }

    static Stream<SanitizeCase> loadSanitizeCases() throws Exception {
        return loadRoot().flatMap(root -> {
            List<SanitizeCase> cases = new ArrayList<>();
            if (root.has("sanitizeName")) {
                for (JsonNode c : root.get("sanitizeName")) {
                    cases.add(new SanitizeCase(
                        c.get("description").asText(),
                        c.get("input").asText(),
                        c.get("expectedOutput").asText()
                    ));
                }
            }
            return cases.stream();
        });
    }

    private static Stream<JsonNode> loadRoot() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = SubpathTraversalTest.class.getResourceAsStream("/test-fixtures/subpath-traversal-cases.json");
        if (is == null) {
            is = java.nio.file.Files.newInputStream(
                java.nio.file.Paths.get("test-fixtures/subpath-traversal-cases.json"));
        }
        return Stream.of(mapper.readTree(is));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadPathCases")
    @DisplayName("Path safety")
    void testPathSafety(PathCase tc) {
        boolean safe = PathUtils.isSubpathSafe(Path.of(tc.base()), Path.of(tc.subpath()));
        assertThat(safe).as(tc.description()).isEqualTo(tc.expectedSafe());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadSanitizeCases")
    @DisplayName("Name sanitization")
    void testSanitizeName(SanitizeCase tc) {
        String result = PathUtils.sanitizeName(tc.input());
        assertThat(result).as(tc.description()).isEqualTo(tc.expectedOutput());
    }
}
