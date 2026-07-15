# Development Notes

This repository follows the miku-soft Java application direction.

## Current State

- Single-module Maven Java application.
- Java source and target compatibility: `1.8`.
- CLI entrypoint: `jp.igapyon.mikutextbundle.cli.MikuTextBundleCli`.
- Core API entrypoint: `jp.igapyon.mikutextbundle.coreapi.TextBundler`.
- Current upstream compatibility target: `miku-text-bundle` `v1.4.0`.
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
- The tool does not auto-detect encodings. Files that cannot be decoded with the selected encoding, or files detected as binary, are skipped and recorded in the final part index section.

## v0.9.0 Collection Policy

- CLI input and output directories are explicit and required: `--input <dir>` and `--output <dir>`.
- Positional input/output arguments, `--input-directory`, `--output-directory`, `--include`, and `--exclude` are removed upstream contracts and should remain rejected.
- File discovery broadly scans regular files under the input directory.
- Known binary extensions, default excluded directories, root `.gitignore`, and the output directory are filtered before reading candidates.
- Ignored directory/file counts are part of the core result and verbose diagnostics.
- Historical v0.9.0 output generated `text-bundle-000-prompt.md`, bundle parts from `text-bundle-001.md` through `text-bundle-998.md`, and terminal `text-bundle-999-index.md`.
- File ordering in bundle parts follows POSIX relative path UTF-16 code unit order without locale or numeric collation.

## v1.0.0 Filename Prefix Policy

- CLI and core API accept `filenamePrefix` / `--filename-prefix`.
- The default prefix is `text-bundle`, preserving the v0.9.0 generated file names.
- Custom prefixes are trimmed and must contain only ASCII letters, digits, `.`, `_`, and `-`.
- Generated file names use the normalized prefix.
- Prompt reading order and the index Parts table must reflect the generated part file names.

## v1.1.0 Compact Output Policy

- The standalone prompt and terminal index files are no longer generated.
- Part files use `text-bundle-001.md` through `text-bundle-999.md`.
- The first part embeds the prompt section and has `prompt: true` front matter.
- The final part embeds the index section and has `terminal: true` front matter.
- A single-part bundle contains both prompt and index sections in `text-bundle-001.md`.
- `BundleResult.promptPath` points to the first part and `BundleResult.indexPath` points to the final part for API compatibility.

## v1.2.0 Dry-run and Readability Policy

- CLI and core API accept `dryRun` / `--dry-run`.
- Dry-run mode performs discovery, skip handling, marker collection, and part planning without creating the output directory or writing generated files.
- Dry-run results return planned output paths, counts, warnings, and `BundleResult.dryRun = true`.
- Part Markdown separates the second and later file chunks with a horizontal rule.

## v1.3.0 and v1.4.0 Part Sizing Policy

- Non-terminal Parts include an acknowledgement footer instructing the receiver to reply only with `OK`.
- The final terminal Part does not include the acknowledgement footer.
- `--max-chars` is the approximate source-content limit used for Part planning; prompt, index, headings, fences, front matter, and acknowledgement overhead are not a fixed rendered-character limit.
- v1.4.0 removes the former fixed `128000` character rendered-Part limit. Choose a smaller `--max-chars` and inspect output when the receiving system has a character limit.

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
