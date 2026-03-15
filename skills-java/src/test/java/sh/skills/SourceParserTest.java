package sh.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sh.skills.providers.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for source URL parsing via provider registry.
 * Driven by test-fixtures/source-parser-cases.json
 */
@DisplayName("Source Parser")
class SourceParserTest {

    record TestCase(String description, String source, String expectedProvider,
                    String expectedOwner, String expectedRepo,
                    String expectedBranch, String expectedPath) {}

    static Stream<TestCase> loadCases() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = SourceParserTest.class.getResourceAsStream("/test-fixtures/source-parser-cases.json");
        if (is == null) {
            is = java.nio.file.Files.newInputStream(
                java.nio.file.Paths.get("test-fixtures/source-parser-cases.json"));
        }
        JsonNode root = mapper.readTree(is);
        List<TestCase> cases = new ArrayList<>();
        for (JsonNode c : root.get("cases")) {
            cases.add(new TestCase(
                c.get("description").asText(),
                c.get("source").asText(),
                c.get("expectedProvider").asText(),
                c.has("expectedOwner") ? c.get("expectedOwner").asText(null) : null,
                c.has("expectedRepo") ? c.get("expectedRepo").asText(null) : null,
                c.has("expectedBranch") && !c.get("expectedBranch").isNull() ? c.get("expectedBranch").asText() : null,
                c.has("expectedPath") && !c.get("expectedPath").isNull() ? c.get("expectedPath").asText() : null
            ));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadCases")
    void testProviderMatching(TestCase tc) {
        HostProvider provider = ProviderRegistry.findProvider(tc.source());

        assertThat(provider)
            .as("Provider found for: " + tc.source())
            .isNotNull();

        assertThat(provider.getSourceType())
            .as(tc.description())
            .isEqualTo(tc.expectedProvider());

        // Additional assertions for GitHub provider
        if ("github".equals(tc.expectedProvider()) && provider instanceof GitHubProvider ghp) {
            GitHubProvider.ParsedGitHubSource parsed = ghp.parse(tc.source());
            if (tc.expectedOwner() != null) {
                assertThat(parsed.owner).as("owner").isEqualTo(tc.expectedOwner());
            }
            if (tc.expectedRepo() != null) {
                assertThat(parsed.repo).as("repo").isEqualTo(tc.expectedRepo());
            }
            assertThat(parsed.branch).as("branch").isEqualTo(tc.expectedBranch());
            assertThat(parsed.skillPath).as("skillPath").isEqualTo(tc.expectedPath());
        }
    }
}
