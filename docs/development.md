# Development Notes

This repository follows the miku-soft Java application direction.

## Current State

- Single-module Maven Java application.
- Java source and target compatibility: `1.8`.
- CLI entrypoint: `jp.igapyon.mikutextbundle.cli.MikuTextBundleCli`.
- Core API entrypoint: `jp.igapyon.mikutextbundle.coreapi.TextBundler`.
- Primary verification command: `mvn test`.
- Upstream repository: `https://github.com/igapyon/miku-text-bundle`.
- Local upstream checkout: `workplace/miku-text-bundle-devel/`.

## Boundaries

- Product semantics belong in the core package.
- CLI code parses arguments, writes stdout/stderr, and returns exit codes.
- `main(String[] args)` is only the outer process boundary.
- Product behavior must not be hidden in repository docs or skill instructions.

## Focused Regression Commands

```sh
mvn test
mvn test -Dtest=MikuTextBundleCliTest
mvn test -Dtest=MikuTextBundleCliIntegrationTest
mvn test -Dtest=TextBundlerTest
mvn test -Dtest=PatternMatcherTest
mvn test -Dtest=MarkdownTest
mvn test -Dtest=PathUtilsTest
mvn test -Dtest=PomMetadataTest
```
