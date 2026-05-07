# Upstream Class Mapping

This document tracks `upstream file -> Java class` mapping for the miku-text-bundle straight conversion.

Upstream repository:

- https://github.com/igapyon/miku-text-bundle

Local upstream checkout:

- `workplace/miku-text-bundle-devel/`

```text
upstream file:
  workplace/miku-text-bundle-devel/src/types.ts

java classes:
  jp.igapyon.mikutextbundle.model.CliOptions
  jp.igapyon.mikutextbundle.model.CollectedFile
  jp.igapyon.mikutextbundle.model.Marker
  jp.igapyon.mikutextbundle.model.SkippedFile
  jp.igapyon.mikutextbundle.model.BundleChunk
  jp.igapyon.mikutextbundle.model.BundlePart
  jp.igapyon.mikutextbundle.coreapi.BundleResult
  jp.igapyon.mikutextbundle.model.EncodingOptions
  jp.igapyon.mikutextbundle.model.SupportedEncoding

notes:
  - POJO classes use public fields during the initial straight conversion.
  - SupportedEncoding and EncodingOptions map the upstream explicit input encoding contract.
```

```text
upstream file:
  workplace/miku-text-bundle-devel/src/cli.ts

java classes:
  jp.igapyon.mikutextbundle.cli.MikuTextBundleCli
  jp.igapyon.mikutextbundle.cli.HelpRequestedException

notes:
  - CLI parses argv and delegates product behavior to core API.
```

```text
upstream file:
  workplace/miku-text-bundle-devel/src/bundler.ts

java classes:
  jp.igapyon.mikutextbundle.coreapi.TextBundler

notes:
  - Core API owns file discovery, skip handling, splitting, Markdown file generation, and result summaries.
```

```text
upstream file:
  workplace/miku-text-bundle-devel/src/markdown.ts

java classes:
  jp.igapyon.mikutextbundle.markdown.Markdown

notes:
  - Initial conversion covers part, index, and prompt Markdown generation.
```

```text
upstream file:
  workplace/miku-text-bundle-devel/src/match.ts

java classes:
  jp.igapyon.mikutextbundle.match.PatternMatcher

notes:
  - Initial conversion covers focused glob and gitignore matching needed by the product.
```

```text
upstream file:
  workplace/miku-text-bundle-devel/src/path-utils.ts

java classes:
  jp.igapyon.mikutextbundle.pathutils.PathUtils

notes:
  - Initial conversion covers POSIX path normalization, extension extraction, and pattern normalization.
```

```text
upstream file:
  workplace/miku-text-bundle-devel/src/main.ts

java classes:
  jp.igapyon.mikutextbundle.cli.MikuTextBundleCli
  jp.igapyon.mikutextbundle.core.MikuTextBundle

notes:
  - Java CLI main entrypoint and small product metadata facade.
```
