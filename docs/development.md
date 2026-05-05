# Development Notes

This repository follows the miku-soft Java application direction.

## Current State

- Single-module Maven Java application.
- Java source and target compatibility: `1.8`.
- CLI entrypoint: `jp.igapyon.mikutextbundle.cli.MikuTextBundleCli`.
- Core API entrypoint: `jp.igapyon.mikutextbundle.coreapi.TextBundler`.
- Primary verification command: `mvn test`.
- Packaged-jar verification command: `mvn verify`.
- Upstream repository: `https://github.com/igapyon/miku-text-bundle`.
- Local upstream checkout: `workplace/miku-text-bundle-devel/`.

## Boundaries

- Product semantics belong in the core package.
- CLI code parses arguments, writes stdout/stderr, and returns exit codes.
- `main(String[] args)` is only the outer process boundary.
- Product behavior must not be hidden in repository docs or skill instructions.

## Packaging

- Release packaging currently targets the executable shaded jar.
- A distribution zip is not needed while the deliverable has no companion launch scripts, native files, or bundled documentation outside the jar and source jar.

## Maven Plugin Decision

- This project does not provide a Maven plugin / Mojo module.
- The primary use case is an explicit CLI run that bundles a repository when a user wants to hand content to generative AI.
- Automatic Maven lifecycle execution is not a primary use case for this tool.
- Maven projects that need integration can call the executable jar or main class explicitly, for example through `exec-maven-plugin`, without adding a first-class plugin module here.

## Focused Regression Commands

```sh
mvn test
mvn verify
mvn test -Dtest=MikuTextBundleCliTest
mvn test -Dtest=MikuTextBundleCliIntegrationTest
mvn verify -Dit.test=MikuTextBundleCliJarIT
mvn test -Dtest=TextBundlerTest
mvn test -Dtest=UpstreamParityTest
mvn test -Dtest=PatternMatcherTest
mvn test -Dtest=MarkdownTest
mvn test -Dtest=PathUtilsTest
mvn test -Dtest=PomMetadataTest
```
