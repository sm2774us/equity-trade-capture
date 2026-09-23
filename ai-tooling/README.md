# AI-Assisted Developer Tooling

Cross-compatible configuration for **Claude Code**, **GitHub Copilot**, and
**OpenAI Codex**, scoped to this repo's architecture (Java 17 / Spring Boot
Kafka services + Angular 18 / Nx frontend) so any of the three tools gives
consistent, repo-aware suggestions.

Works identically on **Windows 11** and **Ubuntu** — all three tools read
plain-text config files from the repo root; nothing here is OS-specific.

## Install locations

| Tool | Config file(s) | Scope |
|---|---|---|
| Claude Code | `CLAUDE.md` (repo root) | Read automatically by `claude` CLI on session start |
| GitHub Copilot | `.github/copilot-instructions.md` | Read automatically by Copilot Chat/completions in VS Code, JetBrains, Visual Studio |
| OpenAI Codex | `AGENTS.md` (repo root) | Read automatically by the Codex CLI/IDE extension |

Each subfolder here (`claude-code/`, `github-copilot/`, `openai-codex/`)
contains the ready-to-copy file plus tool-specific extras (MCP server
config for Claude Code, a settings snippet for Copilot). Copy the relevant
file(s) to the repo root of your clone; they are additive to what's already
described in the main `README.md`.

## Quick install (either OS)

```bash
# From the trading-platform repo root
cp ai-tooling/claude-code/CLAUDE.md .
cp ai-tooling/github-copilot/copilot-instructions.md .github/copilot-instructions.md
cp ai-tooling/openai-codex/AGENTS.md .
```

```powershell
# Windows 11 (PowerShell), from the repo root
Copy-Item ai-tooling\claude-code\CLAUDE.md .
Copy-Item ai-tooling\github-copilot\copilot-instructions.md .github\copilot-instructions.md
Copy-Item ai-tooling\openai-codex\AGENTS.md .
```

## Why one shared prompt structure across three tools

All three files describe the *same* facts about the repo (module layout,
build commands, coding conventions, testing requirements) in each tool's
preferred format, so switching between Claude Code, Copilot, and Codex
mid-task doesn't produce inconsistent suggestions — the core content is
intentionally duplicated, not divergent.
