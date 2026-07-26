# Beta Matrix Results

Status: partial `v0.8.0-dev` execution on 26 July 2026. This records only observed behavior.

## Executed device matrix

| Target | Configuration | Result |
| --- | --- | --- |
| Samsung SM-S938B | Android 16, 1080×2340, releaseVerification | Partial manual smoke passed |
| `PageHarbor_API_36` | Installed API 36 AVD | Blocked: emulator exited before ADB registration |
| Lower-resource API 30–35 | No installed AVD profile | Blocked |
| Tablet / large-screen | No installed or attached target | Blocked |
| API 26/27 boundary | No installed or attached target | Blocked |

## Samsung results

- The debug-signed, minified/resource-shrunk `releaseVerification` build installed and showed
  `0.8.0-dev` / code `8` with the `releaseVerification` build label.
- Home launched without a PageHarbor permission prompt.
- ML Kit Document Scanner launched successfully and exposed gallery import without PageHarbor adding
  camera or storage permissions.
- A temporary synthetic, non-sensitive single-page gallery fixture completed scanner review and
  returned a clean Scan Result with one page and all expected actions.
- JPEG export opened the Android SAF destination picker with `Document-1.jpg`. Cancelling it
  returned to Scan Result with the distinct `Page export cancelled.` state and no crash.

The transient gallery fixtures, screenshots, external scanner UI hierarchy dumps, and device copies
were removed immediately after this run.

## Interruption and recovery

- Scanner launch/cancel and SAF cancellation have manual evidence; rotation/background/process-kill
  during active OCR/searchable-PDF work were not completed in this run.
- The external scanner, picker, and Samsung system UI were usable, but coordinate-only automation
  was not reliable enough to claim OCR/searchable-PDF completion or five consecutive sessions.
- Existing deterministic lifecycle/coordinator coverage remains the evidence for stale callbacks,
  cancellation, destination failure, prepared-output cleanup, and process-reset defaults. It is not
  substituted for the missing manual matrix.

## Temporary-file inventory

- Clean launch after installation: no app-private cache/files/database/shared-preferences files
  observed.
- The partial scanner and cancelled JPEG-export flow created no PageHarbor-owned persistent output.
- The ownership/age cleanup boundaries for stale `searchable-*.pdf` files and stale share copies are
  covered deterministically. A manual active searchable-PDF/process-kill inventory remains planned.

## Defects and limitations

No PageHarbor defect was found in the executed flow. The API 36 emulator launch failure is an
environment limitation, not evidence of API 36 incompatibility. The unexecuted lower-memory,
tablet, lower-API, five-session, multipage OCR, searchable-PDF, share, and active-operation
interruption checks remain beta gaps.
