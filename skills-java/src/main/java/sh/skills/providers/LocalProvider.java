package sh.skills.providers;

import sh.skills.model.Skill;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Provider for local filesystem paths.
 * Handles: ./path, ../path, /absolute/path
 */
public class LocalProvider implements HostProvider {

    private final SkillDiscovery discovery = new SkillDiscovery();

    @Override
    public boolean matches(String source) {
        if (source == null) return false;
        String s = source.trim();
        return s.startsWith("./") || s.startsWith("../") || s.startsWith("/")
            || (s.length() > 2 && s.charAt(1) == ':'); // Windows absolute path
    }

    @Override
    public List<Skill> fetchSkills(String source, Path tempDir) throws ProviderException {
        Path localPath = Paths.get(source.trim()).toAbsolutePath().normalize();
        if (!Files.exists(localPath)) {
            throw new ProviderException("Local path does not exist: " + localPath);
        }
        if (!Files.isDirectory(localPath)) {
            throw new ProviderException("Local path is not a directory: " + localPath);
        }
        return discovery.discover(localPath, null, false);
    }

    @Override
    public String getLatestHash(String source, String skillPath) {
        return null; // Local paths don't have version tracking
    }

    @Override
    public String canonicalUrl(String source) {
        return Paths.get(source.trim()).toAbsolutePath().normalize().toString();
    }

    @Override
    public String getSourceType() { return "local"; }
}
