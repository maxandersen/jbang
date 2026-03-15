# skills-java

**The CLI for the open agent skills ecosystem** — Java port of [vercel-labs/skills](https://github.com/vercel-labs/skills).

Run it with zero Node.js/npm required, using just Java 11+ via [JBang](https://jbang.dev).

## Quick Start

```bash
# Install via JBang (no setup needed)
jbang skills@jbangdev/skills-java add vercel-labs/agent-skills

# Or install JBang first: https://www.jbang.dev/download/
```

## Usage

```bash
# Add skills from a GitHub repo
jbang skills@jbangdev/skills-java add vercel-labs/agent-skills

# Add to specific agents
jbang skills@jbangdev/skills-java add vercel-labs/agent-skills -a claude-code -a cursor

# Non-interactive / CI-friendly
jbang skills@jbangdev/skills-java add vercel-labs/agent-skills --all -y

# List installed skills
jbang skills@jbangdev/skills-java list

# Search for skills
jbang skills@jbangdev/skills-java find web design

# Check for updates
jbang skills@jbangdev/skills-java check

# Update all installed skills
jbang skills@jbangdev/skills-java update

# Remove a skill
jbang skills@jbangdev/skills-java remove web-design

# Create a new skill template
jbang skills@jbangdev/skills-java init my-new-skill
```

## Commands

| Command | Description |
|---------|-------------|
| `add <source>` | Install skills from GitHub, GitLab, or local path |
| `list` (alias: `ls`) | List all installed skills |
| `find [query]` | Search skills at skills.sh |
| `check` | Check for skill updates |
| `update` | Update installed skills |
| `remove` (alias: `rm`) | Remove installed skills |
| `init [name]` | Create a new SKILL.md template |

## Source Formats

```bash
# GitHub shorthand
skills add owner/repo

# Full GitHub URL
skills add https://github.com/owner/repo

# Specific skill within a repo
skills add https://github.com/owner/repo/tree/main/skills/web-design

# GitLab
skills add https://gitlab.com/org/repo

# SSH Git URL
skills add git@github.com:owner/repo.git

# Local path
skills add ./my-local-skills
```

## Supported Agents

All 40+ agents from the original are supported, including:
Claude Code, Cursor, OpenCode, Cline, Codex, Continue, Gemini CLI, GitHub Copilot, Goose, OpenHands, Replit, Roo Code, Windsurf, and many more.

## Options

| Option | Description |
|--------|-------------|
| `-g, --global` | Install/manage globally (`~/<agent>/skills/`) |
| `-a, --agent <name>` | Target specific agent(s) |
| `--skill <name>` | Install only specific skill(s) by name |
| `--all` | Install/remove all without prompting |
| `-y, --yes` | Skip all confirmation prompts (CI mode) |
| `--copy` | Copy files instead of symlinking |
| `--dry-run` | Preview changes without making them |

## Environment Variables

| Variable | Description |
|----------|-------------|
| `GITHUB_TOKEN` | GitHub token for private repos and higher API rate limits |
| `NO_COLOR` | Disable ANSI colors ([no-color.org](https://no-color.org/)) |
| `DO_NOT_TRACK` | Disable anonymous telemetry |
| `SKILLS_TELEMETRY=0` | Disable telemetry |

## Upstream Sync

This project tracks [vercel-labs/skills](https://github.com/vercel-labs/skills) for updates. A weekly GitHub Action checks for upstream changes and opens an issue when updates are available.

The `upstream.lock` file records the last-synced commit SHA.

## Shared Test Fixtures

The `test-fixtures/` directory contains JSON test case files that are shared between this Java implementation and the original TypeScript implementation. This enables cross-implementation test parity.

### Fixture Files

- `skill-matching-cases.json` — Skill name matching/filtering logic
- `source-parser-cases.json` — URL/source parsing for providers
- `subpath-traversal-cases.json` — Path traversal security validation
- `plugin-grouping-cases.json` — Plugin manifest parsing

## Building from Source

```bash
git clone https://github.com/jbangdev/skills-java
cd skills-java
mvn package
java -jar target/skills-java-*.jar --help
```

## License

MIT — Same as the original [vercel-labs/skills](https://github.com/vercel-labs/skills/blob/main/LICENSE).
