package sh.skills.providers;

import sh.skills.model.Skill;
import sh.skills.util.GitUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for GitLab repositories.
 * Handles: https://gitlab.com/owner/repo[/tree/branch/path]
 */
public class GitLabProvider implements HostProvider {

    private static final Pattern GITLAB_URL = Pattern.compile(
        "^https?://gitlab\\.com/([a-zA-Z0-9_.-]+(?:/[a-zA-Z0-9_.-]+)+)(?:/-/tree/([^/]+)(?:/(.+))?)?$"
    );

    private final SkillDiscovery discovery = new SkillDiscovery();

    @Override
    public boolean matches(String source) {
        if (source == null) return false;
        return GITLAB_URL.matcher(source.trim()).matches();
    }

    @Override
    public List<Skill> fetchSkills(String source, Path tempDir) throws ProviderException {
        Matcher m = GITLAB_URL.matcher(source.trim());
        if (!m.matches()) throw new ProviderException("Invalid GitLab URL: " + source);

        String repoPath = m.group(1);
        String branch = m.group(2);
        String skillPath = m.group(3);

        String url = "https://gitlab.com/" + repoPath + ".git";

        try {
            Path cloneDir = GitUtils.cloneToTemp(url, branch);
            try {
                Path searchRoot = skillPath != null ? cloneDir.resolve(skillPath) : cloneDir;
                List<Skill> skills = discovery.discover(searchRoot, null, false);
                copyDirectory(cloneDir, tempDir);
                return skills;
            } finally {
                GitUtils.deleteTempDir(cloneDir.toFile());
            }
        } catch (GitUtils.GitCloneException | IOException e) {
            throw new ProviderException("Failed to fetch from GitLab: " + e.getMessage(), e);
        }
    }

    @Override
    public String getLatestHash(String source, String skillPath) {
        return null; // GitLab hash tracking not yet implemented
    }

    @Override
    public String getSourceType() { return "gitlab"; }

    private void copyDirectory(Path source, Path target) throws IOException {
        java.nio.file.Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                java.nio.file.Files.createDirectories(target.resolve(source.relativize(dir)));
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
}
