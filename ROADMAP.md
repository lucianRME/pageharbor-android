# Roadmap

PageHarbor is in early development. This roadmap is public-facing and intentionally conservative; it does not claim that document scanning or export is production-ready.

## Completed

- Repository and documentation foundation.
- Android Compose foundation.
- Privacy and architecture boundaries.
- Home screen and privacy messaging.
- Branding and design system.
- ML Kit technical spike integration.
- PDF export through Android Storage Access Framework.
- PDF sharing through Android share sheet.

The ML Kit scanner spike is integrated, but real document scanning is not yet validated on a physical Android device.

## In Progress

- Validate ML Kit scanner on a Google Play-enabled emulator and physical Android device.
- Validate first-run module download and offline-after-install behavior.
- Confirm JPEG/PDF result behavior and cancellation handling.

## Planned MVP

- Launch scanner reliably.
- Handle one-page and multi-page results.
- Simple result review.
- Temporary-file cleanup.
- User-safe error handling.
- Accessibility and physical-device validation.

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
