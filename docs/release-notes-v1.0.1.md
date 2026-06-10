# Release Notes: v1.0.1

`v1.0.1` follows upstream `miku-text-bundle` `v1.0.1` and keeps the Java CLI help contract aligned with the Node.js CLI.

## Changes

- Added the Web UI handoff note to `--help`: pasting `<prefix>-000-prompt.md` as the first message body is recommended, not required.
- Added Agent Skill handoff guidance to `text-bundle-999-index.md` when `SKILL.md` or `skills/<skill-name>/SKILL.md` is bundled.
- Added short YAML front matter to generated Markdown files with `tool`, `version`, and `role`.
- Aligned Java `--help` stdout with the Node.js CLI, including trailing newline behavior.
- Updated the project and CLI version to `1.0.1`.
