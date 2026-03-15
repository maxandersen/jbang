package sh.skills.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import sh.skills.model.Skill;
import sh.skills.util.FrontmatterParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provider for RFC 8615 .well-known/skills/ endpoints.
 * Handles: https://example.com/.well-known/skills/ or any https URL that
 * resolves to a skills index.
 */
public class WellKnownProvider implements HostProvider {

    private static final Pattern HTTPS_URL = Pattern.compile("^https?://[^/].*");
    private static final String WELL_KNOWN_PATH = "/.well-known/skills/";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean matches(String source) {
        if (source == null) return false;
        String s = source.trim();
        // Match any https URL that isn't already handled by GitHub/GitLab providers
        return HTTPS_URL.matcher(s).matches()
            && !s.contains("github.com")
            && !s.contains("gitlab.com");
    }

    @Override
    public List<Skill> fetchSkills(String source, Path tempDir) throws ProviderException {
        String baseUrl = normalizeUrl(source);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            // Try .well-known/skills/index.json
            String indexUrl = baseUrl + WELL_KNOWN_PATH + "index.json";
            HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(indexUrl)).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new ProviderException("Well-known endpoint returned " + response.statusCode() + " for " + indexUrl);
            }

            JsonNode index = mapper.readTree(response.body());
            List<Skill> skills = new ArrayList<>();

            if (index.has("skills") && index.get("skills").isArray()) {
                for (JsonNode entry : index.get("skills")) {
                    String skillUrl = entry.has("url") ? entry.get("url").asText() : null;
                    if (skillUrl == null) continue;

                    if (!skillUrl.startsWith("http")) {
                        skillUrl = baseUrl + WELL_KNOWN_PATH + skillUrl;
                    }

                    HttpResponse<String> skillResp = client.send(
                        HttpRequest.newBuilder().uri(URI.create(skillUrl)).timeout(Duration.ofSeconds(10)).build(),
                        HttpResponse.BodyHandlers.ofString()
                    );

                    if (skillResp.statusCode() == 200) {
                        Path skillFile = tempDir.resolve(skillUrl.replaceAll("[^a-zA-Z0-9._-]", "_") + ".md");
                        Files.writeString(skillFile, skillResp.body());
                        Skill skill = FrontmatterParser.parse(skillResp.body(), skillFile);
                        if (skill != null) skills.add(skill);
                    }
                }
            }

            return skills;
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderException("Failed to fetch from well-known endpoint: " + e.getMessage(), e);
        }
    }

    @Override
    public String getLatestHash(String source, String skillPath) {
        return null;
    }

    @Override
    public String getSourceType() { return "wellknown"; }

    private String normalizeUrl(String source) {
        String s = source.trim();
        // Remove trailing slash and well-known path if already present
        s = s.replaceAll(WELL_KNOWN_PATH + ".*$", "");
        s = s.replaceAll("/$", "");
        return s;
    }
}
