# Internal Beta Smoke Test

Use only non-sensitive sample pages. This is a 10–15 minute manual smoke, not a request to upload
anything to Google Play.

Record completed and blocked items in [BETA_MATRIX_RESULTS.md](BETA_MATRIX_RESULTS.md); do not mark
an item passed solely because a deterministic test covers a related boundary.

## Before starting

- Record the version, build code, and build type from **About PageHarbor**.
- Use a local `releaseVerification` build or the approved internal-testing build.
- Do not use IDs, invoices, medical records, personal photos, OCR text, or full file paths.

## Install and update

1. Install the build and open PageHarbor.
2. If a previous internal build is available, install this build over it without uninstalling.
3. Confirm the displayed version is the expected one and Home opens normally.
4. Uninstall, reinstall, and confirm the same Home behavior. Downgrade is intentionally unsupported.

## Core flow

1. Scan one page, then scan multiple pages; try gallery import if the scanner offers it.
2. Cancel the scanner once and confirm PageHarbor remains usable.
3. From Scan Result, save a PDF, share a PDF, export a JPEG page, and save a searchable PDF.
4. Run OCR with English, Romanian diacritics, and German characters if safe sample text is available.
   Open OCR Result and use Copy Text.

## Lifecycle and repeat use

1. Rotate on Scan Result and OCR Result.
2. Background and return to the app once.
3. Cancel a save picker once, retry it, then Discard.
4. Complete a second scan/OCR/export session. Confirm no prior preview, OCR text, filename category,
   progress, or success message appears.

## Report immediately

Report an app close, missing picker, stale page/text, corrupt PDF, wrong filename, OCR failure,
share failure, stuck progress, or a permission prompt. Use
[BUG_REPORT_TEMPLATE.md](BUG_REPORT_TEMPLATE.md); attach screenshots only when they contain no
sensitive content.
