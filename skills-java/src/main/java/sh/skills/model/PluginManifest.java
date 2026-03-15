package sh.skills.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Represents the .claude-plugin/marketplace.json or plugin.json manifest.
 * Mirrors the plugin manifest types from the TypeScript source.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginManifest {

    private Metadata metadata;
    private List<Plugin> plugins;

    public Metadata getMetadata() { return metadata; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }

    public List<Plugin> getPlugins() { return plugins; }
    public void setPlugins(List<Plugin> plugins) { this.plugins = plugins; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        private String pluginRoot;

        public String getPluginRoot() { return pluginRoot; }
        public void setPluginRoot(String pluginRoot) { this.pluginRoot = pluginRoot; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Plugin {
        private String name;
        private String source;
        private List<String> skills;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills; }
    }
}
