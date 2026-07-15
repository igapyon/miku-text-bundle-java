# Release Notes: v1.4.0

`v1.4.0` follows upstream `miku-text-bundle` `v1.4.0` and removes the fixed rendered Markdown Part limit.

## Changes

- Updated the project and CLI version to `1.4.0`.
- Removed the fixed `128000` character limit that was previously applied after a Part was rendered.
- Kept `--max-chars` as the approximate source-content limit used when planning Parts.
- Documented that generated Markdown can exceed `--max-chars` because of headings, fences, front matter, prompt, index, and acknowledgement overhead.

## Compatibility

- CLI options, output format, and existing defaults are unchanged.
- Choose a smaller `--max-chars` and inspect generated files when the receiving system imposes a character limit.

## Verification

- Upstream `npm run build`: passed on 2026-07-15.
- Java `mvn verify`: passed on 2026-07-15.
