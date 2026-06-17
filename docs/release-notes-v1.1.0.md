# Release Notes: v1.1.0

`v1.1.0` follows upstream `miku-text-bundle` devel `v1.1.0` and changes generated Markdown to compact part output.

## Changes

- Updated the project and CLI version to `1.1.0`.
- Stopped generating standalone `text-bundle-000-prompt.md` and `text-bundle-999-index.md` files.
- Embedded the prompt section in the first part file.
- Embedded the terminal index section in the final part file.
- Extended part file numbering through `text-bundle-999.md`.
- Kept `BundleResult.promptPath` and `BundleResult.indexPath` as API fields that point to the first and final part files.
- Updated CLI help, README, tests, and maintenance notes for compact output.

## Compatibility Notes

Scripts that expected separate prompt or index files should read the prompt from the first generated part and the index from the final generated part.
