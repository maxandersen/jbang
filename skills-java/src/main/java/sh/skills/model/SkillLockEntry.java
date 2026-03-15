package sh.skills.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single entry in .skill-lock.json or local-lock.json.
 * Mirrors SkillLockEntry from the TypeScript source.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillLockEntry {
    private String source;
    private String sourceType;
    private String sourceUrl;
    private String skillPath;
    private String skillFolderHash;
    private String installedAt;
    private String updatedAt;

    public SkillLockEntry() {}

    public SkillLockEntry(String source, String sourceType, String sourceUrl,
                          String skillPath, String skillFolderHash) {
        this.source = source;
        this.sourceType = sourceType;
        this.sourceUrl = sourceUrl;
        this.skillPath = skillPath;
        this.skillFolderHash = skillFolderHash;
        this.installedAt = java.time.Instant.now().toString();
        this.updatedAt = this.installedAt;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSkillPath() { return skillPath; }
    public void setSkillPath(String skillPath) { this.skillPath = skillPath; }

    public String getSkillFolderHash() { return skillFolderHash; }
    public void setSkillFolderHash(String skillFolderHash) { this.skillFolderHash = skillFolderHash; }

    public String getInstalledAt() { return installedAt; }
    public void setInstalledAt(String installedAt) { this.installedAt = installedAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
