# Upstream Follow-up Log

This document records concrete upstream checks made while maintaining the Java straight-conversion version.

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
