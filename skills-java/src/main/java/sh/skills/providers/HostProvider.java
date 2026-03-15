package sh.skills.providers;

import sh.skills.model.Skill;

import java.nio.file.Path;
import java.util.List;

/**
 * Provider interface for fetching skills from a source.
 * Mirrors the HostProvider interface from src/providers/types.ts.
 */
public interface HostProvider {

    /**
     * Returns true if this provider can handle the given source string.
     */
    boolean matches(String source);

    /**
     * Fetch skills from the given source into the provided tempDir.
     * Clones or copies skill files into tempDir and returns discovered skills.
     *
     * @param source  The source string (URL, shorthand, path)
     * @param tempDir Working directory for cloned content
     * @return List of discovered skills
     * @throws ProviderException on fetch failure
     */
    List<Skill> fetchSkills(String source, Path tempDir) throws ProviderException;

    /**
     * Get the latest hash/SHA for a specific skill path, used for update detection.
     * Returns null if the provider doesn't support version tracking.
     *
     * @param source    The source string
     * @param skillPath The relative path to the skill within the repo (may be null)
     */
    String getLatestHash(String source, String skillPath) throws ProviderException;

    /**
     * Return a canonical source URL for this source string.
     */
    default String canonicalUrl(String source) { return source; }

    /**
     * Return the source type identifier (e.g. "github", "local", "git").
     */
    String getSourceType();

    class ProviderException extends Exception {
        public ProviderException(String message) { super(message); }
        public ProviderException(String message, Throwable cause) { super(message, cause); }
    }
}
