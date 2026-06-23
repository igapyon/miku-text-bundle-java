# Release Notes: v1.3.0

`v1.3.0` follows upstream `miku-text-bundle` `v1.3.0` and improves practical Part sizing for AI handoff.

## Changes

- Updated the project and CLI version to `1.3.0`.
- Added rendered Markdown length checks so generated Part files stay within the practical `128000` character limit when chunks can be moved to later Parts.
- Included headings, fences, metadata, prompt, index, and acknowledgement overhead in final Part sizing.
- Added an acknowledgement footer to non-terminal Parts instructing the receiver to reply only with `OK`.
- Updated CLI help, README, upstream parity, and focused tests for v1.3.0 behavior.

## Verification

- Upstream `npm run build`: passed on 2026-06-23.
- Java `mvn test`: passed on 2026-06-23.
