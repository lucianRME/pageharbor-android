# Privacy Design and Commitments

PageHarbor is in early development. This document describes the intended architecture and privacy commitments; it is not a final legal privacy policy for Google Play or any other distribution platform.

## Local processing

PageHarbor is designed to process documents locally on the user's Android device. No account or login is required. The project does not intend to include advertising, analytics, tracking, or telemetry.

No document contents should be written to logs. Temporary files should be limited to what core functionality requires and removed when they are no longer needed, including after errors or cancelled operations.

## Storage and sharing

PageHarbor does not operate cloud storage. Users may choose local storage or a third-party storage provider through Android system interfaces, such as the system file picker or share sheet.

When a user chooses a third-party provider, such as Google Drive or OneDrive, PageHarbor passes the user-selected file through the Android system interface. The provider's own privacy policy and practices apply to its handling of that file.

## Permissions and access

Camera and file access will be requested only when needed for core, user-initiated functionality. PageHarbor intends to use scoped Android system access and to avoid the Android `INTERNET` permission.

## Keeping this document current

These statements describe the project's intended design during early development, not verified claims about a completed application. This document must be reviewed and updated if the implementation, permissions, data handling, dependencies, or storage behavior changes.
