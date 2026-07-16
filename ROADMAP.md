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

## In Progress

### `v0.3.0-dev` — Offline OCR Foundation

Planned targets:

- Add bundled ML Kit Text Recognition v2 Latin after dependency and privacy validation. **Implemented:** the bundled, on-device Latin engine processes active-session JPEG pages sequentially and retains in-memory page indexes and failures.
- Introduce a narrow OCR integration boundary and result model. **Implemented:** ML Kit types are confined to `MlKitOcrEngine`.
- Recognize text from scanned JPEG pages and combine it in page order. **Implemented in the scan-result flow; physical scan-flow validation remains in progress.**
- Expose a plain-text preview and allow explicit copying of recognized text. **Implemented:** an in-memory preview is available after explicit user action; copying remains pending.
- Validate English, German, and Romanian recognition.
- Verify OCR operation without a PageHarbor network permission and benchmark representative sample documents.
- Preserve the scan, PDF save, PDF share, and JPEG export flows.

OCR is optional: it must run only after explicit user action and must never block scanning or export. OCR results remain in memory unless the user explicitly copies or exports them. PageHarbor will not introduce cloud OCR or a proprietary backend.

PageHarbor intentionally relies on ML Kit for scanner editing capabilities rather than duplicating crop, rotate, filters, page deletion, or reordering. Use platform capabilities where they are strong. Build only what adds distinct user value.

Implementation sequence:

1. Dependency and privacy validation.
2. OCR engine adapter and result model.
3. Single-page OCR.
4. Multi-page OCR.
5. Copy-text UX.
6. Multilingual benchmark.
7. Physical-device regression validation.

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
