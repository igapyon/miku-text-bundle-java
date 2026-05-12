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
target/miku-text-bundle-java-0.8.0.jar
```

## Quick Start

```sh
java -jar target/miku-text-bundle-java-0.8.0.jar --input . --output out/text-bundle
```

Both `--input` and `--output` are required.

## CLI Usage

```sh
java -jar target/miku-text-bundle-java-0.8.0.jar --input <dir> --output <dir> [options]
```

Options:

| Option | Description | Default |
| --- | --- | --- |
| `--max-chars <number>` | Approximate maximum characters per bundle part. Oversized files may be split. | `120000` |
| `--max-input-file-bytes <number>` | Skip input files larger than this byte size. | `1000000` |
| `--encoding utf-8\|shift_jis` | Set the default input file encoding. | `utf-8` |
| `--encoding-extension ".java=shift_jis"` | Set per-extension input encodings. Multiple rules can be comma-separated. | none |
| `--add-exclude-extension ".ext"` | Add comma-separated file extensions to the exclude list. | default list |
| `--remove-exclude-extension ".ext"` | Remove comma-separated file extensions from the exclude list. | |
| `--add-exclude-directory "dir"` | Add comma-separated directory names or relative paths to the exclude list. | default list |
| `--remove-exclude-directory "dir"` | Remove comma-separated directory names or relative paths from the exclude list. | |
| `--verbose` | Print collection diagnostics. | off |
| `--help` | Print help. | |
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
completed: 1 part(s), 2 file(s) collected, 0 file(s) skipped, 1 directories ignored, 0 file(s) ignored
```

## File Selection

By default, the tool broadly collects regular files under the input directory.

The tool skips:

- Known binary file extensions such as `.png`, `.pdf`, `.zip`, `.xlsx`, and `.jar`
- Default excluded directories such as `.git/`, `.codex/`, `node_modules/`, `dist/`, `build/`, `target/`, and `workplace/`
- Files ignored by the input directory's root `.gitignore`
- Files under the output directory when the output directory is inside the input directory
- Files larger than `--max-input-file-bytes`
- Binary files or files that cannot be decoded with the selected input encoding

## Input Encoding

The default input encoding is UTF-8. Use `--encoding shift_jis` to read collected files as Shift_JIS by default.

Per-extension rules override the default encoding:

```sh
java -jar target/miku-text-bundle-java-0.8.0.jar --input . --output out/text-bundle --encoding utf-8 --encoding-extension ".java=shift_jis,.properties=shift_jis"
```

Supported input encodings are `utf-8` and `shift_jis`. The tool does not auto-detect encodings. Files that cannot be decoded with the selected encoding, or files detected as binary, are skipped and recorded in `text-bundle-000-index.md`.

Use `--add-exclude-extension` and `--remove-exclude-extension` to adjust extension-based filtering:

```sh
java -jar target/miku-text-bundle-java-0.8.0.jar --input . --output out/text-bundle --add-exclude-extension ".wasm,.bin"
java -jar target/miku-text-bundle-java-0.8.0.jar --input . --output out/text-bundle --remove-exclude-extension ".pdf"
```

Use `--add-exclude-directory` and `--remove-exclude-directory` to adjust directory filtering:

```sh
java -jar target/miku-text-bundle-java-0.8.0.jar --input . --output out/text-bundle --add-exclude-directory "generated"
java -jar target/miku-text-bundle-java-0.8.0.jar --input . --output out/text-bundle --remove-exclude-directory "dist"
```

## Examples

Bundle the current repository:

```sh
java -jar target/miku-text-bundle-java-0.8.0.jar --input . --output out/text-bundle
```

Bundle a repository and write to a known directory:

```sh
java -jar target/miku-text-bundle-java-0.8.0.jar --input /path/to/repo --output /path/to/out
```

Show diagnostics:

```sh
java -jar target/miku-text-bundle-java-0.8.0.jar --input . --output out/text-bundle --verbose
```

Use smaller bundle parts:

```sh
java -jar target/miku-text-bundle-java-0.8.0.jar --input . --output out/text-bundle --max-chars 60000
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
- The local upstream checkout used for straight conversion can be placed under `workplace/miku-text-bundle-upstream/` or `workplace/miku-text-bundle-devel/`.
- Only `workplace/.gitkeep` is tracked under `workplace/`.
- macOS metadata, Maven build output, local VS Code MCP settings, and normal `workplace/` contents are ignored by Git.
- `.mvn/jvm.config` is tracked to keep local Maven JVM network settings consistent.

## License

Apache License, Version 2.0.
