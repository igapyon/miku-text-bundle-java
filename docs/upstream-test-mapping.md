# Upstream Test Mapping

This document tracks `upstream test intent -> Java test` mapping for the miku-text-bundle straight conversion.

Upstream repository:

- https://github.com/igapyon/miku-text-bundle

```text
upstream test / intent:
  test/cli.test.ts

java tests:
  jp.igapyon.mikutextbundle.cli.MikuTextBundleCliTest

fixtures:
  inline argv arrays

focused regression:
  mvn test -Dtest=MikuTextBundleCliTest

notes:
  - CLI coverage maps upstream v1.0.0 required `--input` / `--output` parsing.
  - CLI filename prefix coverage maps upstream `--filename-prefix` parser validation.
  - CLI encoding option coverage maps upstream `--encoding` and `--encoding-extension` parser tests.
  - Exclude extension and directory list operation tests map upstream `--add-exclude-*` and `--remove-exclude-*`.
  - v1.5.0 mode parsing, validation, mode-specific prefixes, and help text are covered.
  - Removed positional, `--input-directory`, `--output-directory`, `--include`, `--exclude`, `-h`, and `-v` behavior is covered as rejection cases.
  - Java uses `SupportedEncoding` enum values instead of TypeScript string union literals.
```

```text
upstream test / intent:
  test/cli-subprocess.test.ts

java tests:
  jp.igapyon.mikutextbundle.cli.MikuTextBundleCliIntegrationTest
  jp.igapyon.mikutextbundle.cli.MikuTextBundleCliJarIT

fixtures:
  temporary files from JUnit TempDir

focused regression:
  mvn test -Dtest=MikuTextBundleCliIntegrationTest
  mvn verify -Dit.test=MikuTextBundleCliJarIT

notes:
  - Java test calls the CLI run boundary directly instead of launching a packaged jar.
  - Knowledge source generation and dry-run completion wording are covered.
  - Packaged jar process behavior is covered separately by the Failsafe integration test.
```

```text
upstream test / intent:
  test/match.test.ts

java tests:
  jp.igapyon.mikutextbundle.match.PatternMatcherTest

fixtures:
  inline path and pattern strings

focused regression:
  mvn test -Dtest=PatternMatcherTest
```

```text
upstream test / intent:
  workplace/miku-text-bundle-devel/src/path-utils.ts behavior

java tests:
  jp.igapyon.mikutextbundle.pathutils.PathUtilsTest

fixtures:
  inline paths and patterns

focused regression:
  mvn test -Dtest=PathUtilsTest
```

```text
upstream test / intent:
  test/markdown.test.ts

java tests:
  jp.igapyon.mikutextbundle.markdown.MarkdownTest

fixtures:
  inline model objects and golden Markdown strings

focused regression:
  mvn test -Dtest=MarkdownTest
```

```text
upstream test / intent:
  test/bundler.test.ts

java tests:
  jp.igapyon.mikutextbundle.coreapi.TextBundlerTest
  jp.igapyon.mikutextbundle.coreapi.UpstreamParityTest

fixtures:
  temporary files from JUnit TempDir
  src/test/resources/fixtures/product-repo

focused regression:
  mvn test -Dtest=TextBundlerTest
  mvn test -Dtest=UpstreamParityTest

notes:
  - Extension-specific and default Shift_JIS decoding cases map the upstream explicit input encoding behavior.
  - v1.0.0 broad recursive discovery, default binary extension exclusion, directory exclusion, output-directory exclusion, ignored counters, terminal index, filename prefix, and UTF-16 code unit ordering are covered.
  - Java uses JDK Charset decoding instead of upstream iconv-lite.
  - UpstreamParityTest compares handoff and Knowledge source Markdown with a local upstream `dist/main.js` only when the checkout is version `1.5.0`.
  - A local upstream root can be supplied with `-DmikuTextBundle.upstreamRoot=/path/to/miku-text-bundle`.
  - The parity test is skipped when Node or a local upstream 1.5.0 build is unavailable.
```

```text
upstream test / intent:
  test/package.test.ts

java tests:
  jp.igapyon.mikutextbundle.build.PomMetadataTest

fixtures:
  pom.xml

focused regression:
  mvn test -Dtest=PomMetadataTest

notes:
  - Java test checks Maven project shape, executable jar configuration, and sources jar configuration instead of npm package metadata.
```
