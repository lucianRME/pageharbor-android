# Architecture Decision Records

This log records lightweight product and architecture decisions for PageHarbor. Decisions may be revised as implementation validates platform behavior.

## ADR-001: No Proprietary Backend

Decision:
PageHarbor will not operate a backend for the MVP.

Rationale:
The MVP centers on local document processing and user-controlled save or share destinations. A backend would add privacy, security, operational, and trust costs that are not required for scanning, reviewing, exporting, saving, or sharing.

Consequences:
PageHarbor will not provide account sync, remote backup, server-side processing, or a proprietary document portal in the MVP. Features must work through local processing and Android system interfaces.

## ADR-002: User-Controlled File Destinations

Decision:
Use Android system file-selection interfaces rather than direct cloud-provider integrations.

Rationale:
The Android Storage Access Framework lets the user choose local or provider-backed destinations without PageHarbor integrating cloud SDKs or handling account credentials.

Consequences:
Cloud providers may appear as system picker destinations, but PageHarbor does not manage those accounts or storage systems. Provider-specific behavior, availability, and privacy terms remain outside PageHarbor's control.

## ADR-003: Minimal Architecture

Decision:
Introduce abstractions only when required for state management, testability, or isolation of platform integrations.

Rationale:
The MVP is small, and scanner behavior still needs validation. Premature layers would make the code harder to change without improving user value or privacy.

Consequences:
Static screens should stay simple. Flow coordination, adapters, and helper components should be added when they remove real complexity, isolate platform behavior, or make cleanup and error paths testable.

## ADR-004: System Document Scanner First

Decision:
Evaluate ML Kit Document Scanner before implementing a custom CameraX scanning pipeline.

Rationale:
A system document scanner may reduce custom camera, crop, page detection, and image-processing work while aligning with a user-initiated scan flow. It may also avoid PageHarbor directly requesting camera permission, depending on validated scanner behavior.

Risks:

- Google Play Services dependency.
- Component availability.
- Initial component download.
- Emulator limitations.
- Limitations on scanner customization.
- Privacy wording.

Validation criteria:

- Successful multi-page scan.
- Cancellation handling.
- Output format and URI behavior.
- Operation without PageHarbor declaring INTERNET.
- Behavior after required components are already installed.
- Behavior on a physical Android device.
- Compatibility with minimum supported Android version.

Consequences:
Implementation should avoid public claims of absolute offline scanning until scanner behavior is tested. If the system scanner does not satisfy privacy, availability, quality, or UX requirements, PageHarbor may revisit a custom scanner approach.

## ADR-005: No Internal Document Library In MVP

Decision:
PageHarbor will export documents but will not initially maintain a permanent internal document collection.

Rationale:
The MVP focuses on scanning, review, PDF export, and user-selected save or share. An internal library would require retention rules, backup behavior, indexing, deletion UX, and additional privacy review.

Consequences:
Users control saved files through their chosen destinations. PageHarbor must avoid retaining document copies beyond the active scan and export workflow except where strictly required for retry, sharing, or cleanup.

## ADR-006: Grantable URIs For PDF Sharing

Decision:
Share ML Kit PDF results directly when they use a grantable `content` URI. When ML Kit returns a readable but non-grantable URI, copy the PDF byte-for-byte into PageHarbor's private cache and share it through a narrowly scoped `FileProvider` content URI.

Rationale:
Physical-device validation showed that ML Kit can return a PDF URI that PageHarbor can read but Android cannot safely grant to another application. Raw file locations must not be exposed, and share targets require temporary read access to a content URI.

Consequences:
The FileProvider exposes only the `shared-pdfs` cache directory. Partial copies are deleted after preparation failures. Completed copies are eligible for operating-system cache eviction and PageHarbor removes copies at least 24 hours old when the app starts. No storage permission, network access, or re-encoding is introduced.
