# PageHarbor

PageHarbor is an open-source, privacy-first Android document scanner. The app is under development and is not production-ready.

## Current status

The latest completed milestone is `v0.2.0-dev`. Scanning, PDF save, PDF share, and JPEG page export are implemented and physically validated on Samsung Android 16.

The current development version is `v0.3.0-dev`, focused on the Review Experience. The exact PageHarbor-owned review scope remains subject to technical validation because ML Kit already provides some review behavior inside its scanner UI.

See [ROADMAP.md](ROADMAP.md) for the current project roadmap.

## Core commitments

- Offline-first document processing
- No ads
- No tracking or analytics
- No account or login
- Open-source development

## Planned MVP

- Scan one or more pages
- Review and reorder pages
- Export scans as a PDF
- Export scanned pages individually
- Save files through the Android system file picker
- Share files through the Android share sheet

## Planned technology stack

- Kotlin
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL
- ML Kit Document Scanner
- Android Storage Access Framework

PageHarbor is intended to process documents locally on the device and let users choose where files are saved or shared. Cloud providers such as Google Drive or OneDrive may appear as destinations through the Android system file picker. PageHarbor does not operate proprietary cloud storage or directly access those services.

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
