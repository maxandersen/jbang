package sh.skills.providers;

import sh.skills.model.Skill;
import sh.skills.util.GitUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provider for generic Git URLs (SSH or HTTPS non-GitHub/GitLab).
 * Handles: git@github.com:owner/repo.git, git@gitlab.com:owner/repo.git,
 *          https://bitbucket.org/owner/repo.git, etc.
 */
public class GitProvider implements HostProvider {

    private static final Pattern GIT_SSH = Pattern.compile("^git@[^:]+:.+\\.git$");
    private static final Pattern GIT_HTTPS = Pattern.compile("^https?://.+\\.git$");

    private final SkillDiscovery discovery = new SkillDiscovery();

    @Override
    public boolean matches(String source) {
        if (source == null) return false;
        String s = source.trim();
        return GIT_SSH.matcher(s).matches() || GIT_HTTPS.matcher(s).matches();
    }

    @Override
    public List<Skill> fetchSkills(String source, Path tempDir) throws ProviderException {
        try {
            Path cloneDir = GitUtils.cloneToTemp(source.trim(), null);
            try {
                List<Skill> skills = discovery.discover(cloneDir, null, false);
                copyDirectory(cloneDir, tempDir);
                return skills;
            } finally {
                GitUtils.deleteTempDir(cloneDir.toFile());
            }
        } catch (GitUtils.GitCloneException | IOException e) {
            throw new ProviderException("Failed to clone git repo: " + e.getMessage(), e);
        }
    }

    @Override
    public String getLatestHash(String source, String skillPath) {
        return null;
    }

    @Override
    public String getSourceType() { return "git"; }

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
