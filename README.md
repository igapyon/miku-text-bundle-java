# miku-text-bundle-java

`miku-text-bundle-java` is the Java CLI repository for the `miku-text-bundle` miku-soft project.

This repository is currently initialized as a Java application skeleton. The product core contract, upstream mapping, and concrete input/output behavior are still pending.

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
java -jar target/miku-text-bundle-java-0.1.0-SNAPSHOT.jar --help
java -jar target/miku-text-bundle-java-0.1.0-SNAPSHOT.jar --version
```

The CLI currently exposes only the initial help and version contract. Product commands will be added after the `miku-text-bundle` input/output specification is settled.

## Repository Operation

- `docs/` contains miku-soft design documents and project maintenance notes.
- `workplace/` is for local upstream checkouts, temporary verification data, generated files, and other scratch work.
- Only `workplace/.gitkeep` is tracked under `workplace/`.
- macOS metadata, Maven build output, local VS Code MCP settings, and normal `workplace/` contents are ignored by Git.
- `.mvn/jvm.config` is tracked to keep local Maven JVM network settings consistent.

## License

Apache License, Version 2.0.
