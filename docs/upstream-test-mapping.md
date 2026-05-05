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
  test/bundler.test.ts

java tests:
  jp.igapyon.mikutextbundle.coreapi.TextBundlerTest

fixtures:
  temporary files from JUnit TempDir

focused regression:
  mvn test -Dtest=TextBundlerTest
```
