# Large Document Validation

Status: `v0.6.0-dev` in progress.

## Baseline

Scanner JPEG URIs are processed sequentially by OCR. OCR opens each source stream through the engine boundary; searchable-PDF generation opens one JPEG stream per page, embeds it in the in-progress PDFBox document, and checks coroutine cancellation between pages. SAF output is copied with an 8 KiB bounded buffer and does not load the completed PDF into a byte array.

Scanner acquisition remains limited to the existing 10-page setting. The 20-page regression below exercises the post-scan processing engine only; it does not claim 20- or 50-page scanner acquisition support.

## Regression coverage

- A deterministic 20-page searchable-PDF instrumentation test reuses one generated non-sensitive JPEG stream rather than committing duplicate binaries.
- It verifies page count, ordered text extraction, and normal generator cleanup.
- Temporary 20- and 50-page JPEG packs were generated outside the repository with fixed synthetic identifiers and text, copied to a device-visible validation folder, and removed from host and device after the gallery-import check could not reliably proceed.

## Current risks and limits

- PDFBox retains the in-progress document until it is saved. This is the main remaining memory-pressure risk for large scans; no speculative image-quality reduction was added.
- OCR text and geometry grow with page count for the active session only and are discarded with the session.
- Cancellation is checked before each OCR/generation/copy iteration. Prepared output is deleted after cancellation, failure, success, or explicit discard.
- Physical 20- and 50-page scanner acquisition, OCR/export timings, memory observations, cancellation/lifecycle behavior, and low-storage/provider-failure measurements remain pending. Samsung NotificationShade retained input focus during the gallery-import attempt. No low-end-device claim or recommended increase beyond the existing 10-page scanner limit is made.

## Stress classification

The 20-page generator check is a normal connected regression test. Physical 20- and 50-page scanner validation is a future manual task on an environment where system UI does not retain focus; it is not silently included in the normal suite.
