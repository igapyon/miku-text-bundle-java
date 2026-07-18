# Upstream Class Mapping

This document tracks `upstream file -> Java class` mapping for the miku-text-bundle straight conversion.

Upstream repository:

- https://github.com/igapyon/miku-text-bundle

Local upstream checkout:

- `workplace/miku-text-bundle-upstream/` or `workplace/miku-text-bundle-devel/`

```text
upstream file:
  workplace/miku-text-bundle-devel/src/types.ts

java classes:
  jp.igapyon.mikutextbundle.model.CliOptions
  jp.igapyon.mikutextbundle.model.CollectedFile
  jp.igapyon.mikutextbundle.model.Marker
  jp.igapyon.mikutextbundle.model.SkippedFile
  jp.igapyon.mikutextbundle.model.IgnoreStats
  jp.igapyon.mikutextbundle.model.BundleChunk
  jp.igapyon.mikutextbundle.model.BundlePart
  jp.igapyon.mikutextbundle.model.BundleMode
  jp.igapyon.mikutextbundle.coreapi.BundleResult
  jp.igapyon.mikutextbundle.model.EncodingOptions
  jp.igapyon.mikutextbundle.model.SupportedEncoding

notes:
  - POJO classes use public fields during the initial straight conversion.
  - SupportedEncoding and EncodingOptions map the upstream explicit input encoding contract.
  - CliOptions maps the required `--input` / `--output` contract, `--mode`, `--filename-prefix`, and exclude-list controls.
  - BundleMode, BundleChunk provenance fields, and BundleResult Knowledge source paths map the v1.5.0 additive contract.
  - IgnoreStats and BundleResult map the v1.0.0 ignored-directory / ignored-file counters.
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
  - v1.5.0 keeps handoff reserve/render behavior separate from Knowledge source files and their management index.
```

```text
upstream file:
  workplace/miku-text-bundle-devel/src/discovery.ts

java classes:
  jp.igapyon.mikutextbundle.discovery.FileDiscovery
  jp.igapyon.mikutextbundle.discovery.DiscoveryResult

notes:
  - Java discovery follows the v1.0.0 broad recursive collection contract and POSIX relative path UTF-16 code unit ordering.
  - Default exclude extensions and directories live in FileDiscovery so the core layer does not depend on the CLI adapter.
```

```text
upstream file:
  workplace/miku-text-bundle-devel/src/markdown.ts

java classes:
  jp.igapyon.mikutextbundle.markdown.Markdown

notes:
  - Covers handoff part/index/prompt Markdown and v1.5.0 neutral Knowledge source and management-index rendering.
  - v1.6.0 shares explicit Agent-readable file blocks, language metadata, path display escaping, and source-whitespace preservation across both output modes.
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
