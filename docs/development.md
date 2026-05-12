# Development Notes

This repository follows the miku-soft Java application direction.

## Current State

- Single-module Maven Java application.
- Java source and target compatibility: `1.8`.
- CLI entrypoint: `jp.igapyon.mikutextbundle.cli.MikuTextBundleCli`.
- Core API entrypoint: `jp.igapyon.mikutextbundle.coreapi.TextBundler`.
- Current upstream compatibility target: `miku-text-bundle` `v0.8.0`.
- Primary verification command: `mvn test`.
- Packaged-jar verification command: `mvn verify`.
- Upstream repository: `https://github.com/igapyon/miku-text-bundle`.
- Local upstream checkout: `workplace/miku-text-bundle-upstream/` or `workplace/miku-text-bundle-devel/`.

## Boundaries

- Product semantics belong in the core package.
- CLI code parses arguments, writes stdout/stderr, and returns exit codes.
- `main(String[] args)` is only the outer process boundary.
- Product behavior must not be hidden in repository docs or skill instructions.

## Encoding Policy

- Input decoding supports explicit `utf-8` and `shift_jis` selection.
- The default encoding is `utf-8`.
- Extension rules such as `.java=shift_jis` override the default encoding for exact final extensions.
- The tool does not auto-detect encodings. Files that cannot be decoded with the selected encoding, or files detected as binary, are skipped and recorded in the index Markdown.

## v0.8.0 Collection Policy

- CLI input and output directories are explicit and required: `--input <dir>` and `--output <dir>`.
- Positional input/output arguments, `--input-directory`, `--output-directory`, `--include`, and `--exclude` are removed upstream contracts and should remain rejected.
- File discovery broadly scans regular files under the input directory.
- Known binary extensions, default excluded directories, root `.gitignore`, and the output directory are filtered before reading candidates.
- Ignored directory/file counts are part of the core result and verbose diagnostics.

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
mvn test -Dtest=UpstreamParityTest -DmikuTextBundle.upstreamRoot=/path/to/miku-text-bundle
mvn test -Dtest=PatternMatcherTest
mvn test -Dtest=MarkdownTest
mvn test -Dtest=PathUtilsTest
mvn test -Dtest=PomMetadataTest
```
