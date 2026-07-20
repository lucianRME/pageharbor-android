# PageHarbor

PageHarbor is an open-source, privacy-first Android document scanner. The app is under development and is not production-ready.

## Current status

The completed development milestone is `v0.4.0-dev`. PageHarbor supports offline scanning, PDF save and share, JPEG page export, explicit on-device Latin OCR with in-memory selectable text and Copy Text, and searchable-PDF generation. Searchable PDFs are rebuilt locally from scanned JPEG pages with an invisible OCR text layer, then saved through the Android system file picker. The UI is organized into focused Home, Scan Result, and OCR Result surfaces. ML Kit's scanner provides crop, rotate, filters, and multi-page review; PageHarbor does not retain an internal document library.

See [ROADMAP.md](ROADMAP.md) for the current project roadmap.

## Core commitments

- Offline scanning and document processing
- No ads
- No tracking or analytics
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

## Planned technology stack

- Kotlin
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL
- ML Kit Document Scanner
- Android Storage Access Framework

PageHarbor's privacy-first architecture keeps document processing on the device and lets users choose where files are saved or shared. Cloud providers such as Google Drive or OneDrive may appear only as destinations through the Android system file picker. PageHarbor does not operate proprietary cloud storage, use cloud OCR or AI services, or directly access those services.

## Local development

```sh
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

## License

PageHarbor is licensed under the [Apache License 2.0](LICENSE).

## Attribution

Developed by Lucian Irimie and published under SynapseWorks.
