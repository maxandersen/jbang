package sh.skills.agents;

import sh.skills.model.AgentConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Registry of all supported coding agents.
 * Mirrors src/agents.ts from the TypeScript source.
 * Includes all 40+ agents with their skills directory paths.
 */
public class AgentRegistry {

    private static final List<AgentConfig> AGENTS = Collections.unmodifiableList(Arrays.asList(
        // Claude Code
        new AgentConfig("claude-code", "Claude Code",
            ".claude/skills", ".claude/skills", ".claude"),

        // Cursor
        new AgentConfig("cursor", "Cursor",
            ".cursor/skills", ".cursor/skills", ".cursor"),

        // OpenCode
        new AgentConfig("opencode", "OpenCode",
            ".opencode/skills", ".opencode/skills", ".opencode"),

        // Cline
        new AgentConfig("cline", "Cline",
            ".cline/skills", ".cline/skills", ".cline"),

        // Codex (OpenAI)
        new AgentConfig("codex", "Codex",
            ".codex/skills", ".codex/skills", ".codex"),

        // CodeBuddy
        new AgentConfig("codebuddy", "CodeBuddy",
            ".codebuddy/skills", ".codebuddy/skills", ".codebuddy"),

        // Command Code
        new AgentConfig("commandcode", "Command Code",
            ".commandcode/skills", ".commandcode/skills", ".commandcode"),

        // Continue
        new AgentConfig("continue", "Continue",
            ".continue/skills", ".continue/skills", ".continue"),

        // Cortex
        new AgentConfig("cortex", "Cortex",
            ".cortex/skills", ".cortex/skills", ".cortex"),

        // Crush
        new AgentConfig("crush", "Crush",
            ".crush/skills", ".crush/skills", ".crush"),

        // Droid
        new AgentConfig("droid", "Droid",
            ".droid/skills", ".droid/skills", ".droid"),

        // Gemini CLI
        new AgentConfig("gemini-cli", "Gemini CLI",
            ".gemini/skills", ".gemini/skills", ".gemini"),

        // GitHub Copilot
        new AgentConfig("github-copilot", "GitHub Copilot",
            ".github/skills", ".github/skills", ".github/copilot-instructions.md"),

        // Goose
        new AgentConfig("goose", "Goose",
            ".goose/skills", ".goose/skills", ".goose"),

        // iFlow CLI
        new AgentConfig("iflow-cli", "iFlow CLI",
            ".iflow/skills", ".iflow/skills", ".iflow"),

        // Kimi CLI
        new AgentConfig("kimi-cli", "Kimi CLI",
            ".kimi/skills", ".kimi/skills", ".kimi"),

        // Kiro CLI
        new AgentConfig("kiro-cli", "Kiro CLI",
            ".kiro/skills", ".kiro/skills", ".kiro"),

        // Kode
        new AgentConfig("kode", "Kode",
            ".kode/skills", ".kode/skills", ".kode"),

        // MCPJam
        new AgentConfig("mcpjam", "MCPJam",
            ".mcpjam/skills", ".mcpjam/skills", ".mcpjam"),

        // Mistral Vibe
        new AgentConfig("mistral-vibe", "Mistral Vibe",
            ".mistral/skills", ".mistral/skills", ".mistral"),

        // Mux
        new AgentConfig("mux", "Mux",
            ".mux/skills", ".mux/skills", ".mux"),

        // Neovate
        new AgentConfig("neovate", "Neovate",
            ".neovate/skills", ".neovate/skills", ".neovate"),

        // OpenClaw
        new AgentConfig("openclaw", "OpenClaw",
            ".openclaw/skills", ".openclaw/skills", ".openclaw"),

        // OpenHands
        new AgentConfig("openhands", "OpenHands",
            ".openhands/skills", ".openhands/skills", ".openhands"),

        // Pi
        new AgentConfig("pi", "Pi",
            ".pi/skills", ".pi/skills", ".pi"),

        // Qoder
        new AgentConfig("qoder", "Qoder",
            ".qoder/skills", ".qoder/skills", ".qoder"),

        // Qwen Code
        new AgentConfig("qwen-code", "Qwen Code",
            ".qwen/skills", ".qwen/skills", ".qwen"),

        // Replit
        new AgentConfig("replit", "Replit",
            ".replit/skills", ".replit/skills", ".replit"),

        // Roo Code
        new AgentConfig("roo-code", "Roo Code",
            ".roo/skills", ".roo/skills", ".roo"),

        // Trae
        new AgentConfig("trae", "Trae",
            ".trae/skills", ".trae/skills", ".trae"),

        // Trae CN
        new AgentConfig("trae-cn", "Trae CN",
            ".trae-cn/skills", ".trae-cn/skills", ".trae-cn"),

        // Windsurf
        new AgentConfig("windsurf", "Windsurf",
            ".windsurf/skills", ".windsurf/skills", ".windsurf"),

        // Zencoder
        new AgentConfig("zencoder", "Zencoder",
            ".zencoder/skills", ".zencoder/skills", ".zencoder"),

        // Pochi
        new AgentConfig("pochi", "Pochi",
            ".pochi/skills", ".pochi/skills", ".pochi"),

        // AdaL
        new AgentConfig("adal", "AdaL",
            ".adal/skills", ".adal/skills", ".adal"),

        // Amp
        new AgentConfig("amp", "Amp",
            ".amp/skills", ".amp/skills", ".amp"),

        // Augment
        new AgentConfig("augment", "Augment",
            ".augment/skills", ".augment/skills", ".augment"),

        // Junie
        new AgentConfig("junie", "Junie",
            ".junie/skills", ".junie/skills", ".junie"),

        // Kilo
        new AgentConfig("kilo", "Kilo",
            ".kilo/skills", ".kilo/skills", ".kilo"),

        // Warp
        new AgentConfig("warp", "Warp",
            ".warp/skills", ".warp/skills", ".warp")
    ));

    public static List<AgentConfig> getAgents() {
        return AGENTS;
    }

    /**
     * Find an agent by its name identifier.
     */
    public static Optional<AgentConfig> findByName(String name) {
        return AGENTS.stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Returns agents that appear to be installed on this machine.
     */
    public static List<AgentConfig> getInstalledAgents() {
        return AGENTS.stream()
                .filter(AgentConfig::isInstalled)
                .collect(java.util.stream.Collectors.toList());
    }
}
