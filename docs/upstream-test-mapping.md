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
  - UpstreamParityTest compares generated Markdown with local `workplace/miku-text-bundle-devel/dist/main.js`.
  - The parity test is skipped when Node or the local upstream checkout is unavailable.
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
