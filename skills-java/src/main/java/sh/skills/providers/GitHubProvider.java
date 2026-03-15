package sh.skills.providers;

import sh.skills.model.Skill;
import sh.skills.util.GitUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for GitHub repositories.
 * Handles:
 *   - "owner/repo" shorthand
 *   - "https://github.com/owner/repo"
 *   - "https://github.com/owner/repo/tree/branch/path/to/skill"
 */
public class GitHubProvider implements HostProvider {

    // Matches: owner/repo (no slashes beyond the first)
    private static final Pattern SHORTHAND = Pattern.compile("^[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+$");
    // Matches: https://github.com/owner/repo or https://github.com/owner/repo/tree/branch/...
    private static final Pattern GITHUB_URL = Pattern.compile(
        "^https?://github\\.com/([a-zA-Z0-9_.-]+)/([a-zA-Z0-9_.-]+)(?:/tree/([^/]+)(?:/(.+))?)?$"
    );

    private final SkillDiscovery discovery = new SkillDiscovery();

    @Override
    public boolean matches(String source) {
        if (source == null) return false;
        return SHORTHAND.matcher(source.trim()).matches()
            || GITHUB_URL.matcher(source.trim()).matches();
    }

    @Override
    public List<Skill> fetchSkills(String source, Path tempDir) throws ProviderException {
        ParsedGitHubSource parsed = parse(source);
        String url = "https://github.com/" + parsed.owner + "/" + parsed.repo + ".git";

        try {
            Path cloneDir = GitUtils.cloneToTemp(url, parsed.branch);
            try {
                // If a skill path was specified, look only there
                Path searchRoot = parsed.skillPath != null
                    ? cloneDir.resolve(parsed.skillPath)
                    : cloneDir;
                return discovery.discover(searchRoot, null, false);
            } finally {
                // Don't delete here - caller manages lifetime via tempDir
                // Copy to tempDir instead
                copyDirectory(cloneDir, tempDir);
                GitUtils.deleteTempDir(cloneDir.toFile());
            }
        } catch (GitUtils.GitCloneException | IOException e) {
            throw new ProviderException("Failed to fetch from GitHub: " + e.getMessage(), e);
        }
    }

    @Override
    public String getLatestHash(String source, String skillPath) throws ProviderException {
        ParsedGitHubSource parsed = parse(source);
        String branch = parsed.branch != null ? parsed.branch : "HEAD";
        String path = skillPath != null ? skillPath : (parsed.skillPath != null ? parsed.skillPath : "");

        String apiUrl = "https://api.github.com/repos/" + parsed.owner + "/" + parsed.repo
            + "/git/trees/" + branch + "?recursive=0";
        if (!path.isEmpty()) {
            apiUrl = "https://api.github.com/repos/" + parsed.owner + "/" + parsed.repo
                + "/contents/" + path + "?ref=" + branch;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github.v3+json")
                    .timeout(Duration.ofSeconds(10));

            String token = System.getenv("GITHUB_TOKEN");
            if (token != null && !token.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> response = client.send(reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Extract sha from the response
                String body = response.body();
                int shaIdx = body.indexOf("\"sha\":");
                if (shaIdx >= 0) {
                    int start = body.indexOf('"', shaIdx + 6) + 1;
                    int end = body.indexOf('"', start);
                    if (start > 0 && end > start) {
                        return body.substring(start, end);
                    }
                }
            }
        } catch (Exception e) {
            // Return null if we can't get the hash (non-fatal)
        }
        return null;
    }

    @Override
    public String canonicalUrl(String source) {
        ParsedGitHubSource parsed = parse(source);
        return "https://github.com/" + parsed.owner + "/" + parsed.repo;
    }

    @Override
    public String getSourceType() { return "github"; }

    public ParsedGitHubSource parse(String source) {
        source = source.trim();
        Matcher urlMatcher = GITHUB_URL.matcher(source);
        if (urlMatcher.matches()) {
            return new ParsedGitHubSource(
                urlMatcher.group(1),
                urlMatcher.group(2),
                urlMatcher.group(3),
                urlMatcher.group(4)
            );
        }
        // Shorthand owner/repo
        int slash = source.indexOf('/');
        return new ParsedGitHubSource(source.substring(0, slash), source.substring(slash + 1), null, null);
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        java.nio.file.Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                java.nio.file.Files.createDirectories(targetDir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                java.nio.file.Files.copy(file, target.resolve(source.relativize(file)),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    public static class ParsedGitHubSource {
        public final String owner;
        public final String repo;
        public final String branch;
        public final String skillPath;

        public ParsedGitHubSource(String owner, String repo, String branch, String skillPath) {
            this.owner = owner;
            this.repo = repo;
            this.branch = branch;
            this.skillPath = skillPath;
        }
    }
}
