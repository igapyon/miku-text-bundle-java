# Development Notes

This repository follows the miku-soft Java application direction.

## Current State

- Single-module Maven Java application.
- Java source and target compatibility: `1.8`.
- CLI entrypoint: `jp.igapyon.mikutextbundle.cli.MikuTextBundleCli`.
- Core package root: `jp.igapyon.mikutextbundle.core`.
- Primary verification command: `mvn test`.

## Boundaries

- Product semantics belong in the core package.
- CLI code parses arguments, writes stdout/stderr, and returns exit codes.
- `main(String[] args)` is only the outer process boundary.
- Product behavior must not be hidden in repository docs or skill instructions.

## Pending Product Contract

The `miku-text-bundle` input/output contract is not yet defined in this skeleton. Until that is settled, the CLI intentionally supports only `--help` and `--version`.
