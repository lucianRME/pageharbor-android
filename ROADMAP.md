# Roadmap

PageHarbor is in early development. This roadmap is public-facing and intentionally conservative; it does not claim that document scanning or export is production-ready.

## Completed

- Repository and documentation foundation.
- Android Compose foundation.
- Privacy and architecture boundaries.
- Home screen and privacy messaging.
- Branding and design system.
- ML Kit technical spike integration.
- `v0.2.0-dev` export milestone:
  - Save PDF through Android Storage Access Framework.
  - Share PDF through the Android share sheet.
  - Export Pages as JPEG through Android Storage Access Framework.
  - Physical Samsung validation on Android 16.

Document scanning, PDF save, PDF share, and JPEG page export have been validated on a physical Samsung device.

## In Progress

### `v0.3.0-dev` — Review Experience

Planned targets:

- Preview scanned pages.
- Show page count and ordering.
- Remove a page where technically supported.
- Reorder pages where technically supported.
- Rotate pages where technically supported.
- Rescan or return to the scanner where technically practical.
- Ensure reviewed content is what gets saved, shared, and exported.

ML Kit already provides some review behavior inside its scanner UI. The exact scope of a PageHarbor-owned review experience still requires technical validation. PageHarbor does not currently own or edit page image content after ML Kit returns it.

## Planned MVP

- Review one-page and multi-page scan results.
- Temporary-file cleanup.
- User-safe error handling.
- Accessibility validation.

## Later Considerations

These are non-committed possibilities:

- OCR.
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
