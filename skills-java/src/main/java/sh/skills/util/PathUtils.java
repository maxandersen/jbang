package sh.skills.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Security-critical path utilities.
 * Ports sanitizeName() and isSubpathSafe() from the TypeScript source.
 */
public class PathUtils {

    /**
     * Sanitize a skill name to prevent path traversal and injection.
     * Strips dangerous characters and enforces kebab-case-like naming.
     */
    public static String sanitizeName(String name) {
        if (name == null) return "";

        // Remove null bytes
        name = name.replace("\0", "");

        // Normalize path separators and remove traversal sequences
        name = name.replace("\\", "/");

        // Take only the last path component (prevent directory traversal)
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        // Remove any remaining dots at start (hidden files / relative traversal)
        while (name.startsWith(".")) {
            name = name.substring(1);
        }

        // Allow only alphanumeric, hyphens, underscores, dots (not at start)
        name = name.replaceAll("[^a-zA-Z0-9\\-_.]", "-");

        // Collapse consecutive hyphens
        name = name.replaceAll("-{2,}", "-");

        // Remove trailing/leading hyphens
        name = name.replaceAll("^-+|-+$", "");

        return name;
    }

    /**
     * Verify that the resolved path is safely within the expected base directory.
     * Prevents path traversal attacks during skill extraction.
     */
    public static boolean isSubpathSafe(Path basePath, Path subPath) {
        try {
            Path normalizedBase = basePath.toRealPath();
            Path normalizedSub = subPath.normalize().toAbsolutePath();
            // Use startsWith on normalized paths
            return normalizedSub.startsWith(normalizedBase);
        } catch (IOException e) {
            // If we can't resolve the real path, use normalize only
            Path normalizedBase = basePath.normalize().toAbsolutePath();
            Path normalizedSub = subPath.normalize().toAbsolutePath();
            return normalizedSub.startsWith(normalizedBase);
        }
    }

    /**
     * Returns the XDG data home directory.
     * Follows XDG Base Directory specification.
     */
    public static Path xdgDataHome() {
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome != null && !xdgDataHome.isEmpty()) {
            return Paths.get(xdgDataHome);
        }
        return Paths.get(System.getProperty("user.home"), ".local", "share");
    }

    /**
     * Returns the XDG config home directory.
     */
    public static Path xdgConfigHome() {
        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        if (xdgConfigHome != null && !xdgConfigHome.isEmpty()) {
            return Paths.get(xdgConfigHome);
        }
        return Paths.get(System.getProperty("user.home"), ".config");
    }

    /**
     * Returns the global skill lock file path (~/.skill-lock.json).
     */
    public static Path globalSkillLockPath() {
        return Paths.get(System.getProperty("user.home"), ".skill-lock.json");
    }
}
