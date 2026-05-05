# miku-text-bundle-java

`miku-text-bundle-java` is the Java CLI repository for the `miku-text-bundle` miku-soft project.

This repository is a Java straight-conversion version of the upstream `miku-text-bundle` project.

Upstream:

- https://github.com/igapyon/miku-text-bundle

## Requirements

- Java 8 compatible source and bytecode
- Maven

## Build

```sh
mvn test
mvn package
```

The packaged CLI jar is created under `target/`.

## CLI

```sh
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar --help
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar --version
java -jar target/miku-text-bundle-java-0.5.0-SNAPSHOT.jar <inputDir> [outputDir] --max-chars 120000
```

The CLI collects repository text files and generates split Markdown bundle files.

## Repository Operation

- `docs/` contains miku-soft design documents and project maintenance notes.
- `workplace/` is for local upstream checkouts, temporary verification data, generated files, and other scratch work.
- The local upstream checkout used for straight conversion can be placed under `workplace/miku-text-bundle-devel/`.
- Only `workplace/.gitkeep` is tracked under `workplace/`.
- macOS metadata, Maven build output, local VS Code MCP settings, and normal `workplace/` contents are ignored by Git.
- `.mvn/jvm.config` is tracked to keep local Maven JVM network settings consistent.

## License

Apache License, Version 2.0.
