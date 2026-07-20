# Roadmap

PageHarbor is in early development. This roadmap is public-facing and intentionally conservative; it does not claim that document scanning or export is production-ready.

## Completed

- Repository and documentation foundation.
- Android Compose foundation.
- Privacy and architecture boundaries.
- Home screen and privacy messaging.
- Branding and design system.
- ML Kit technical spike integration.
- ML Kit scanner integration, including multi-page scanning and its built-in crop, rotate, filters, deletion, and reordering experience.
- `v0.2.0-dev` export milestone:
  - Save PDF through Android Storage Access Framework.
  - Share PDF through the Android share sheet.
  - Export Pages as JPEG through Android Storage Access Framework.
  - Physical Samsung validation on Android 16.

Document scanning, PDF save, PDF share, and JPEG page export have been validated on a physical Samsung device.

## Completed milestones

### `v0.3.0-dev` — Offline OCR Foundation

- Add bundled ML Kit Text Recognition v2 Latin after dependency and privacy validation. **Implemented:** the bundled, on-device Latin engine processes active-session JPEG pages sequentially and retains in-memory page indexes and failures.
- Introduce a narrow OCR integration boundary and result model. **Implemented:** ML Kit types are confined to `MlKitOcrEngine`.
- Recognize text from scanned JPEG pages and combine it in page order. **Implemented in the scan-result flow; physical scan-flow validation remains in progress.**
- Expose a plain-text preview and allow explicit copying of recognized text. **Implemented:** text remains in memory and is copied only through explicit user action.
- Preserve page ordering, empty/partial-result handling, bounded decode, and local-only processing.
- Preserve the scan, PDF save, PDF share, and JPEG export flows.

OCR is optional: it must run only after explicit user action and must never block scanning or export. OCR results remain in memory unless the user explicitly copies or exports them. PageHarbor will not introduce cloud OCR or a proprietary backend.

The current UI keeps Home focused on starting a scan. Scan Result owns export and OCR actions plus their feedback, while OCR Result owns in-memory recognized-text actions. There is no bottom navigation or internal document library.

PageHarbor intentionally relies on ML Kit for scanner editing capabilities rather than duplicating crop, rotate, filters, page deletion, or reordering. Use platform capabilities where they are strong. Build only what adds distinct user value.

### `v0.4.0-dev` — Searchable PDF

- OCR geometry captured in an engine-neutral layout model.
- Local searchable-PDF generator that rebuilds pages from JPEG images.
- Invisible Unicode OCR text layer with embedded font support.
- Unicode extraction and selection validation for English, Romanian, and German fixtures.
- Local export orchestration, including OCR, generation, write stages, cancellation, and private-cache cleanup.
- Save searchable PDFs through the Android Storage Access Framework.
- User-facing Scan Result flow for saving a searchable PDF.
- Android and desktop Chrome compatibility validation, plus performance smoke measurements.

Searchable-PDF generation remains local to the active scan session. It does not add cloud OCR, a proprietary backend, an internal document library, or automatic cloud sync. Adobe Acrobat and managed Google Drive viewer validation remain pending because those viewers were unavailable or constrained in the validation environment.

### `v0.5.0-dev` — Smart Document Output

Completed:

- Deterministic local document classification for invoice, receipt, letter, form, and unknown categories.
- Unicode-aware English, German, and Romanian matching with conservative unknown fallback.
- Privacy-preserving category-only filename suggestions for searchable-PDF SAF export.
- User-editable suggested filename passed only to the system picker; no duplicate tracking, filename history, metadata, or OCR-derived filename content.
- Samsung manual validation of all five suggested filenames, user filename override, SAF cancellation, and retry behavior.
- Privacy and automated validation, including 74 passing connected tests with no failures, errors, or skips.

PDF metadata was intentionally excluded from `v0.5.0-dev`. Alternate SAF-provider behavior, duplicate-name provider behavior, spoken TalkBack verification, 200% font verification, and external Adobe/Google Drive viewer checks remain documented validation gaps, not known product defects.

### `v0.6.0-dev` — Planned

Planned scope only; no implementation is committed by this roadmap.

## Planned MVP

- Review one-page and multi-page scan results.
- Temporary-file cleanup.
- User-safe error handling.
- Accessibility validation.

## Later Considerations

These are non-committed possibilities:

- Document organization.
- Additional privacy-preserving features.

## Explicit Non-Goals For MVP

- Accounts.
- Proprietary backend.
- Automatic cloud sync.
- Advertising.
- Analytics.
- Subscriptions.
- Internal permanent document library.
- Smart document output, deterministic filename suggestions, or improved metadata in `v0.4.0-dev`.
- Document search or an internal library in `v0.4.0-dev`.
