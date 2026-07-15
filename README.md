# PageHarbor

PageHarbor is an open-source, privacy-first Android document scanner. The project is in early development. The Android project foundation exists, but document-scanning features are planned and are not yet claimed to be implemented.

## Current status

The Android Compose foundation, privacy documentation, architecture boundaries, and initial branding system are in place. ML Kit Document Scanner integration is under technical validation. Saving and sharing a returned scanned PDF through Android system interfaces are implemented, but the broader scanning workflow still requires physical-device validation before release.

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
- Save files through the Android system file picker
- Share files through the Android share sheet

## Planned technology stack

- Kotlin
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL
- ML Kit Document Scanner, subject to final implementation validation
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
