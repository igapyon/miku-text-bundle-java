# Release Notes: v1.6.0

`v1.6.0` follows the upstream Agent-readable file-block contract in both output modes. This intentionally changes the stable Markdown format; consumers that parse older headings or provenance lines must migrate to the new format.

## Changes

- Added `### FILE: <path>` headings and matching `BEGIN FILE` / `END FILE` boundaries for every collected file or split chunk.
- Added `Source code block`, `Source text block`, or neutral `Source content block` labels and human-readable language names.
- Expanded language mapping for common programming, markup, configuration, and structured-text extensions and special filenames.
- Applied the same file-block renderer to `handoff` and `knowledge-source` modes.
- Added chunk number and original source line range to split blocks.
- Preserved repeated blank lines and other source-body whitespace.
- Preserved collision-safe tilde fence extension.
- Escaped backslashes and control characters in displayed paths and made index code spans safe for pipes and backticks.
- Updated the project and CLI version from `1.5.0` to `1.6.0`.

## Compatibility

- The former handoff heading `### <path>` is replaced by `### FILE: <path>` and explicit boundaries.
- The former per-block character counts, source counts, split-warning prose, and thematic separators are removed. Exact mapping remains in the terminal index and result model.
- The former Knowledge source `## Source:`, `Source path`, and `Source chunk` provenance block is replaced by the shared file-block contract.
- Markdown and plain-text source bodies now use labeled tilde fences instead of raw Markdown sections.
