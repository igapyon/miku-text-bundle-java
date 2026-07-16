# Release Notes: v1.5.0

`v1.5.0` follows upstream `miku-text-bundle` `v1.5.0` and adds Knowledge source output while preserving the established handoff contract.

## Changes

- Added `--mode handoff|knowledge-source`; the default remains `handoff`.
- Added neutral numbered Knowledge source Markdown with source-path and split-range provenance.
- Added a separate management index for effective configuration, source mapping, diagnostics, markers, and stale output candidates.
- Added mode-specific default prefixes: `text-bundle` for handoff and `knowledge` for Knowledge source output.
- Added dry-run, stale-output, CLI, splitting, and Node-vs-Java parity regressions for the new mode.

Format conversion, service registration, and registration-target size constraints remain outside this tool's scope.
