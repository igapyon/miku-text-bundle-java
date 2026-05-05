# miku-text-bundle-java

`miku-text-bundle-java` is a Java CLI that collects text files from a repository and writes them as split Markdown bundle files for generative AI handoff.

It is useful when a repository is too large to paste into an AI chat in one message. The tool creates an index, one or more bundle parts, and a prompt file that describes the reading order.

This repository is a Java straight-conversion version of the upstream TypeScript project:

- https://github.com/igapyon/miku-text-bundle

## Requirements

- Java 8 or later
- Maven 3.x

## Build

```sh
mvn test
mvn package
```

The executable jar is created under `target/`.

```text
target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar
```

## Quick Start

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar .
```

When the output directory is omitted, files are written under:

```text
workplace/miku-text-bundle/<yyyyMMddHHmm>/
```

To choose the output directory explicitly:

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar . out/text-bundle
```

## CLI Usage

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar <inputDir> [outputDir] [options]
```

Named directory options are also supported:

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar --input-directory <dir> --output-directory <dir>
```

Options:

| Option | Description | Default |
| --- | --- | --- |
| `--max-chars <number>` | Approximate maximum characters per bundle part. Oversized files may be split. | `120000` |
| `--max-input-file-bytes <number>` | Skip input files larger than this byte size. | `1000000` |
| `--include <glob>` | Add comma-separated include patterns, such as `docs/**/*.md,package.json`. | none |
| `--exclude <glob>` | Add comma-separated exclude patterns. | none |
| `--verbose` | Print collection diagnostics. | off |
| `--help`, `-h` | Print help. | |
| `--version` | Print version. | |

## Output Files

Each run generates:

| File | Purpose |
| --- | --- |
| `text-bundle-000-index.md` | Summary, part list, skipped files, warnings, and TODO/FIXME/XXX markers. |
| `text-bundle-000-prompt.md` | Prompt that tells the receiver how to read the bundle files. |
| `text-bundle-001.md`, `text-bundle-002.md`, ... | Collected source and text content split into Markdown parts. |

The CLI prints generated file paths and a completion summary:

```text
generated: /path/to/out/text-bundle-000-index.md
generated: /path/to/out/text-bundle-001.md
generated: /path/to/out/text-bundle-000-prompt.md
completed: 1 part(s), 2 file(s) collected
```

## File Selection

By default, the tool collects:

- Root files: `README.md`, `TODO.md`
- Source directories: `src/`, `lib/`, `app/`, `test/`, `tests/`
- Source extensions: `ts`, `tsx`, `js`, `jsx`, `mjs`, `cjs`, `java`, `cs`

The tool skips:

- Root dot directories such as `.git/`, `.idea/`, and `.secret/`
- Files ignored by the root `.gitignore`
- Files matching `--exclude`
- Files larger than `--max-input-file-bytes`
- Binary or non-UTF-8 files

Use `--include` to add extra text files:

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar . out/text-bundle --include "docs/**/*.md,pom.xml"
```

Use `--exclude` to remove matching files from the collected set:

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar . out/text-bundle --exclude "src/generated/**"
```

## Examples

Bundle the current repository with the default output path:

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar .
```

Bundle a repository and write to a known directory:

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar /path/to/repo /path/to/out
```

Include Markdown docs and show diagnostics:

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar . out/text-bundle --include "docs/**/*.md" --verbose
```

Use smaller bundle parts:

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar . out/text-bundle --max-chars 60000
```

## Development

Primary verification:

```sh
mvn test
```

Full verification, including packaged-jar process tests:

```sh
mvn verify
```

## Repository Operation

- `docs/` contains miku-soft design documents and project maintenance notes.
- `workplace/` is for local upstream checkouts, temporary verification data, generated files, and other scratch work.
- The local upstream checkout used for straight conversion can be placed under `workplace/miku-text-bundle-devel/`.
- Only `workplace/.gitkeep` is tracked under `workplace/`.
- macOS metadata, Maven build output, local VS Code MCP settings, and normal `workplace/` contents are ignored by Git.
- `.mvn/jvm.config` is tracked to keep local Maven JVM network settings consistent.

## License

Apache License, Version 2.0.
