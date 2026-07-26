# Device Compatibility Matrix

PageHarbor has `minSdk 26` (Android 8) and `targetSdk 36`. This matrix records evidence; it does
not claim compatibility for devices that have not been tested.

| Device / Android version | Form factor | Status | Coverage |
| --- | --- | --- | --- |
| Samsung SM-S938B / Android 16 | Phone | Manually validated | Prior core/lifecycle releaseVerification smoke; v0.8 local update and clean-install launch |
| API 36 emulator | Phone emulator | Automated / previously manually validated | Connected UI and lifecycle coverage; rerun for beta when available |
| Android 8–9 | Phone | Planned | Minimum-SDK boundary smoke |
| Android 10 | Phone | Planned | Manual smoke |
| Android 12 | Phone | Planned | Manual smoke |
| Android 13 | Pixel or equivalent | Planned | Manual smoke |
| Android 14 | Phone | Planned | Manual smoke |
| Android 15 | Phone | Planned | Manual smoke |
| Android 16 | Pixel | Planned | Manual smoke |
| Lower-memory Android device | Phone | Planned | Core flow and cancellation smoke |
| Tablet / large-screen Android device | Tablet | Planned | Layout, dialogs, and action reachability |

Statuses: **automated** means deterministic suite coverage; **manually validated** means a recorded
human smoke; **planned** has no compatibility claim; **unavailable** means no test device exists.
