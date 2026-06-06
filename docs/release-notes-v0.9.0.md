# Release Notes: v0.9.0

`v0.9.0` follows upstream `miku-text-bundle` `v0.9.0` and updates the Text Bundle reading order and terminal index contract.

## Changes

- Changed the index file name from `text-bundle-000-index.md` to `text-bundle-999-index.md`.
- Treat `text-bundle-999-index.md` as the final file in the reading order.
- Changed the prompt reading order to `text-bundle-000-prompt.md`, bundle part files, then `text-bundle-999-index.md`.
- Removed the `END_OF_TEXT_BUNDLE` completion signal from the prompt contract.
- Reserved `text-bundle-999-index.md`, limiting bundle part files to `text-bundle-001.md` through `text-bundle-998.md`.
- Clarified bundle file ordering as POSIX relative path UTF-16 code unit order.

## Tests

- Updated prompt and CLI tests for the terminal index file name.
- Added regression coverage for UTF-16 code unit file ordering.
- Added regression coverage for the reserved `text-bundle-999-index.md` part limit.
