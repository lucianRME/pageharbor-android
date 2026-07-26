# Device Compatibility Matrix

PageHarbor has `minSdk 26` (Android 8) and `targetSdk 36`. This matrix records evidence; it does
not claim compatibility for devices that have not been tested.

| Device / Android version | Form factor | Status | Coverage |
| --- | --- | --- | --- |
| Samsung SM-S938B / Android 16 | Phone | Partially validated | v0.8 update/clean install, releaseVerification Home, gallery-import scanner, Scan Result, cancelled JPEG SAF export |
| API 36 emulator | Phone emulator | Blocked | Installed AVD exited before ADB registration during beta rerun |
| Android 8–9 | Phone | Blocked | No installed/attached minimum-SDK target |
| Android 10 | Phone | Planned | Manual smoke |
| Android 12 | Phone | Planned | Manual smoke |
| Android 13 | Pixel or equivalent | Planned | Manual smoke |
| Android 14 | Phone | Planned | Manual smoke |
| Android 15 | Phone | Planned | Manual smoke |
| Android 16 | Pixel | Planned | Manual smoke |
| Lower-memory Android device | Phone | Blocked | No installed lower-resource emulator profile or attached device |
| Tablet / large-screen Android device | Tablet | Blocked | No installed/attached tablet or resizable target |

Statuses: **automated** means deterministic suite coverage; **manually validated** means a recorded
human smoke; **planned** has no compatibility claim; **unavailable** means no test device exists.
