# Release Notes: v1.0.0

`v1.0.0` follows upstream `miku-text-bundle` `v1.0.0` and adds custom generated file prefixes for multi-bundle AI handoff workflows.

## Highlights

- Added `--filename-prefix <prefix>`.
- The default generated file names remain `text-bundle-000-prompt.md`, `text-bundle-001.md`, and `text-bundle-999-index.md`.
- When a prefix is specified, prompt, part, and index file names use that prefix.
- The prompt reading order and index Parts table reflect the generated file names.
- Expanded `--help` as a short runtime contract for AI agents and scripts.
- Updated the project and CLI version to `1.0.0`.

## Filename Prefix

Example:

```sh
java -jar target/miku-text-bundle-java-1.0.0.jar --input . --output out/text-bundle --filename-prefix my-repo-text-bundle
```

Generated files:

```text
my-repo-text-bundle-000-prompt.md
my-repo-text-bundle-001.md
my-repo-text-bundle-999-index.md
```

The prefix is trimmed and must contain only ASCII letters, digits, `.`, `_`, and `-`.

## Verification

- `mvn test`
- `mvn verify`
