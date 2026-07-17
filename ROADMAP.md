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

### `v0.4.0-dev` — Planned work

- Searchable PDF.
- Smart filename.
- Current-document OCR search.
- OCR highlighting.
- Future document organization.

## Planned MVP

- Review one-page and multi-page scan results.
- Temporary-file cleanup.
- User-safe error handling.
- Accessibility validation.

## Later Considerations

These are non-committed possibilities:

- Searchable PDFs.
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
- Searchable PDF in `v0.3.0-dev`.
- Smart naming in `v0.3.0-dev`.
- Document search or a library in `v0.3.0-dev`.
