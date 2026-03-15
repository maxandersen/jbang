package sh.skills.util;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Git utilities using JGit.
 * Ports the git clone functionality from src/git.ts.
 */
public class GitUtils {

    /** Default clone timeout in seconds (matches TypeScript 60s) */
    private static final int CLONE_TIMEOUT_SECONDS = 60;

    /**
     * Shallow-clone a repository to a temporary directory.
     * Returns the path to the cloned directory.
     *
     * @param url  The repository URL to clone
     * @param branch  Optional branch name (null for default branch)
     * @return Path to the temporary clone
     * @throws GitCloneException on clone failure
     */
    public static Path cloneToTemp(String url, String branch) throws GitCloneException {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("skills-clone-");
        } catch (IOException e) {
            throw new GitCloneException("Failed to create temp directory: " + e.getMessage(), e);
        }

        // Validate temp dir is safely within system temp
        Path systemTemp = Path.of(System.getProperty("java.io.tmpdir"));
        if (!PathUtils.isSubpathSafe(systemTemp, tempDir)) {
            throw new GitCloneException("Temp directory is outside system temp: " + tempDir);
        }

        try {
            CloneCommand cmd = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(tempDir.toFile())
                    .setDepth(1)
                    .setCloneAllBranches(false)
                    .setTimeout(CLONE_TIMEOUT_SECONDS);

            if (branch != null && !branch.isEmpty()) {
                cmd.setBranch(branch);
            }

            // Check for GitHub token
            String token = System.getenv("GITHUB_TOKEN");
            if (token != null && !token.isEmpty() && url.contains("github.com")) {
                cmd.setCredentialsProvider(
                    new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider("x-access-token", token)
                );
            }

            try (Git git = cmd.call()) {
                // Clone successful
            }

            return tempDir;
        } catch (TransportException e) {
            deleteTempDir(tempDir.toFile());
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Auth") || msg.contains("auth") || msg.contains("401") || msg.contains("403"))) {
                throw new GitCloneException("Authentication failed cloning " + url + ". " +
                    "Set GITHUB_TOKEN environment variable for private repos.", e);
            }
            if (msg != null && msg.contains("timeout")) {
                throw new GitCloneException("Clone timed out for " + url + " after " + CLONE_TIMEOUT_SECONDS + "s.", e);
            }
            throw new GitCloneException("Failed to clone " + url + ": " + msg, e);
        } catch (GitAPIException e) {
            deleteTempDir(tempDir.toFile());
            throw new GitCloneException("Failed to clone " + url + ": " + e.getMessage(), e);
        }
    }

    /** Delete a temp directory recursively, ignoring errors */
    public static void deleteTempDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteTempDir(child);
                } else {
                    child.delete();
                }
            }
        }
        dir.delete();
    }

    public static class GitCloneException extends Exception {
        public GitCloneException(String message) { super(message); }
        public GitCloneException(String message, Throwable cause) { super(message, cause); }
    }
}
