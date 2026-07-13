# Privacy Boundaries

PageHarbor is intended to keep document handling local and user-controlled. This document defines intended boundaries for future implementation and highlights behavior that must be validated before public claims are strengthened.

## Document Data Lifecycle

- Captured pages originate from the system scanner.
- Scan results are handled locally by PageHarbor.
- Temporary files exist only as long as required for review, PDF preparation, save, share, retry, or cleanup.
- Exported files are written only to a destination chosen by the user.
- PageHarbor does not retain its own cloud copy.
- Document contents must not be logged.

Temporary file ownership and cleanup must be explicit in implementation, including error and cancellation paths.

## External Components

The intended scanner may be provided through Google Play Services.

Assumptions requiring implementation validation:

- Scanning and processing are intended to occur on-device.
- Required scanner components may need to be downloaded by Google Play Services.
- PageHarbor itself should not require the INTERNET permission for this flow.
- Google Play Services is an external platform dependency with its own behavior and privacy terms.
- Public privacy wording must reflect the real first-run and ongoing scanner behavior observed during implementation.

Do not claim absolute offline behavior until implementation testing confirms the real behavior on physical devices.

## User-Selected Cloud Providers

Google Drive, OneDrive, Dropbox, or other providers may appear in Android's system file picker.

- PageHarbor should not integrate cloud-provider SDKs for the MVP.
- PageHarbor should not receive provider account credentials.
- PageHarbor should write only to the destination selected through the Android system interface.
- When the user chooses a provider, that provider handles storage under its own terms.

## Backup

Application-managed private scan data should not be included in Android automatic backup.

If temporary or private document files are introduced, implementation must verify that backup rules exclude them or that they are not stored in locations eligible for backup.

## Logging And Debugging

Do not log:

- Document images.
- OCR text.
- File names.
- File paths containing personal information.
- Document metadata.
- Content URIs.
- Share destinations.

Allowed diagnostics should be limited to technical error categories that contain no document content, no user-selected destination details, and no sensitive metadata.

## Permissions

Intended permission approach:

- No INTERNET permission.
- No broad storage permission.
- No account permission.
- No location, contacts, microphone, phone, or advertising permissions.
- Camera permission should not be added if the selected system scanner does not require PageHarbor to request it directly.

Any future permission must be justified by a user-initiated core feature and reviewed against a platform alternative.
