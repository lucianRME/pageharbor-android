# PageHarbor

PageHarbor is an open-source, privacy-first Android document scanner. The project is in early development; the features described below are planned and are not yet claimed to be implemented.

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

## License

PageHarbor is licensed under the [Apache License 2.0](LICENSE).

## Attribution

Developed by Lucian Irimie and published under SynapseWorks.
