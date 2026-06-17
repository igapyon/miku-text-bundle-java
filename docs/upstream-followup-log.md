# Upstream Follow-up Log

This document records concrete upstream checks made while maintaining the Java straight-conversion version.

## 2026-06-17: Follow `miku-text-bundle` devel v1.1.0

- Upstream source checked: `https://github.com/igapyon/miku-text-bundle`, devel branch commit `57f70cd82f442376a02593c72d409fef7d994407`.
- Upstream release status checked: latest GitHub Release remains `v1.0.1`; devel `package.json` version is `1.1.0`.
- Main upstream changes:
  - standalone `000-prompt` and `999-index` files were replaced by compact part output
  - first part embeds the prompt section
  - final part embeds the terminal index section
  - part file numbers now run through `999`
- Java changes made:
  - project and CLI version updated to `1.1.0`
  - core output generation now writes compact part files only
  - `BundleResult.promptPath` and `BundleResult.indexPath` now point to the first and final part files
  - CLI help, README, release notes, and regression tests updated for compact output
- Verification:
  - upstream Node `npm run build`: passed on 2026-06-17
  - Java `mvn test`: passed on 2026-06-17
  - Java `mvn verify`: passed on 2026-06-17
  - `UpstreamParityTest` with `-DmikuTextBundle.upstreamRoot=workplace/miku-text-bundle-upstream`: passed on 2026-06-17

## 2026-06-10: Follow `miku-text-bundle` v1.0.1

- Upstream source checked: local `miku-text-bundle` workspace updated to `1.0.1`.
- Java version updated to `1.0.1`.
- CLI `--help` text aligned with the Node.js runtime, including the Web UI `000-prompt` handoff recommendation.

## 2026-06-09: Follow `miku-text-bundle` v1.0.0

- Upstream source checked: `https://github.com/igapyon/miku-text-bundle`, tag `v1.0.0`.
- Upstream tag commit: `2b0a60ef012a0c715818e85fb0918b86f89e9fc9`.
- Upstream checkout updated locally under `workplace/miku-text-bundle-upstream/`.
- Main upstream changes:
  - `--filename-prefix <prefix>` was added
  - generated prompt, part, and index file names can use a custom prefix
  - prompt reading order reflects the generated file names
  - `--help` was expanded into a short runtime contract for agents and scripts
- Java changes made:
  - project and CLI version updated to `1.0.0`
  - CLI parser and core API now validate and apply filename prefixes
  - prompt Markdown generation now receives actual prompt and index file names
  - tests added for CLI parsing, core generation, CLI integration, and packaged jar prefix behavior
- Verification:
  - Java `mvn test`: passed on 2026-06-09
  - Java `mvn verify`: passed on 2026-06-09
  - `UpstreamParityTest`: skipped because a local upstream `v1.0.0` build was unavailable

## 2026-06-06: Follow `miku-text-bundle` v0.9.0

- Upstream source checked: `https://github.com/igapyon/miku-text-bundle`, tag `v0.9.0`.
- Upstream checkout created locally under `workplace/miku-text-bundle-upstream/`.
- Main upstream changes:
  - index file name changed from `text-bundle-000-index.md` to terminal `text-bundle-999-index.md`
  - prompt reading order changed to prompt, bundle parts, then terminal index
  - `END_OF_TEXT_BUNDLE` was removed from the prompt contract
  - `text-bundle-999-index.md` is reserved, limiting bundle part files to `text-bundle-001.md` through `text-bundle-998.md`
  - bundle file order is POSIX relative path UTF-16 code unit order
- Java changes made:
  - project and CLI version updated to `0.9.0`
  - terminal index name, generated-path output order, and prompt contract updated
  - part count guard added for the reserved terminal index file name
  - discovery ordering changed to Java `String.compareTo`, matching UTF-16 code unit order
  - focused regression tests added or updated for prompt order, index name, part reservation, and file ordering
- Verification:
  - Java `mvn test`: passed on 2026-06-06
  - Java `mvn verify`: passed on 2026-06-06
  - `UpstreamParityTest`: skipped because the local upstream checkout does not contain built `dist/main.js`

## 2026-05-12: Follow `miku-text-bundle` v0.8.0

- Upstream source checked: `https://github.com/igapyon/miku-text-bundle`, tag `v0.8.0`.
- Upstream build checked locally under `/private/tmp/miku-text-bundle-upstream`.
- Main upstream change: CLI input/output selection and file discovery were revised for v0.8.0.
- Java changes made:
  - project and CLI version updated to `0.8.0`
  - CLI now requires `--input` and `--output`
  - positional arguments, `--input-directory`, `--output-directory`, `--include`, `--exclude`, `-h`, and `-v` are rejected
  - broad recursive discovery added through `jp.igapyon.mikutextbundle.discovery`
  - default exclude extension and directory lists added
  - ignored file/directory counters added to the core result and verbose diagnostics
- Accepted Java runtime difference:
  - Java uses JDK charset decoding, while upstream uses Node / `iconv-lite`.
- Verification:
  - upstream Node `npm run build`: passed
  - Java `mvn test`: passed
