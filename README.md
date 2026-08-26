# PageHarbor

PageHarbor is an open-source, privacy-first Android document scanner. The app is under development and is not production-ready.

## Current status

`v0.8.0-dev` is complete and tagged. `1.0.0` is the current production-release preparation
build; it is not yet available through Google Play and does not claim universal device/provider
compatibility. The completed milestone established controlled internal-beta readiness, local
update-path validation, defect reproducibility, and device compatibility evidence without changing
the product feature set. PageHarbor supports local document scanning, PDF
save and share, JPEG page export, explicit on-device Latin OCR with in-memory selectable text and
Copy Text, searchable-PDF generation, and deterministic category-only filename suggestions for
searchable-PDF saves. Searchable PDFs are rebuilt locally from scanned JPEG pages with an invisible
OCR text layer, then saved through the Android system file picker. The user can edit the suggested
name; the SAF provider remains authoritative for the final name and destination. The UI is organized
into focused Home, Scan Result, and OCR Result surfaces. OCR Result presents the selected page's
local preview and recognized text with multipage navigation, without creating a document library.
The active in-memory session retains completed Scan Result and OCR Result state across rotation only.
ML Kit's scanner provides crop, rotate, filters, and multi-page review.

See [ROADMAP.md](ROADMAP.md) for the current project roadmap.

## Core commitments

- On-device document and OCR content processing
- No ads
- No PageHarbor-operated tracking or analytics
- No account or login
- Open-source development

## Implemented MVP capabilities

- Scan one or more pages
- Review and reorder pages
- Export scans as a PDF and save it through the Android Storage Access Framework (SAF)
- Share PDFs through the Android share sheet
- Export scanned pages individually as JPEG through SAF
- Run OCR locally, review in-memory recognized text, and copy text explicitly
- Generate a local searchable PDF with an invisible Unicode OCR text layer and save it through SAF
- Suggest a safe searchable-PDF filename from a broad local category: invoice, receipt, letter, form, or unknown
- Keep the completed active scan and selected OCR page available across configuration changes; process-death recovery is intentionally unsupported

## Planned technology stack

- Kotlin
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL
- ML Kit Document Scanner
- Android Storage Access Framework

PageHarbor's privacy-first architecture keeps document processing on the device and lets users choose where files are saved or shared. Filename suggestions never contain OCR-derived names, dates, amounts, identifiers, addresses, or other document values. PDF metadata is intentionally excluded from `v0.5.0-dev`. Cloud providers such as Google Drive or OneDrive may appear only as destinations through the Android system file picker. PageHarbor does not operate proprietary cloud storage, use cloud OCR or AI services, or directly access those services.

Google ML Kit is a separately licensed Google SDK. Its documented technical diagnostics can be
encrypted and sent to Google, but document images and recognized text are processed on-device and
are not sent by ML Kit. See [PRIVACY.md](PRIVACY.md) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) before distribution.

Scanner acquisition remains limited to the validated 10-page setting. The post-scan searchable-PDF engine has deterministic 20-page regression coverage, but PageHarbor does not claim universal external viewer or SAF-provider compatibility, full accessibility certification, low-end-device validation, or process-death recovery.

## Local development

```sh
./gradlew assembleDebug
./gradlew bundleRelease
./gradlew bundleReleaseVerification
./gradlew test
./gradlew lint
```

See [docs/RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md) for unsigned/debug-signed local verification and future Play upload signing. No signing credential belongs in this repository.

Internal beta preparation is documented in [docs/BETA_SMOKE_TEST.md](docs/BETA_SMOKE_TEST.md),
[docs/INTERNAL_TESTER_GUIDE.md](docs/INTERNAL_TESTER_GUIDE.md), and
[docs/BUG_REPORT_TEMPLATE.md](docs/BUG_REPORT_TEMPLATE.md). Do not use sensitive documents in
beta testing.

## License

PageHarbor is licensed under the [Apache License 2.0](LICENSE).

## Attribution

Developed by Lucian Irimie and published under SynapseWorks.
