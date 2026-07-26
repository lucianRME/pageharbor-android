# Beta Matrix Results

Status: partial `v0.8.0-dev` execution on 26 July 2026. This records only observed behavior.

## Executed device matrix

| Target | Configuration | Result |
| --- | --- | --- |
| Samsung SM-S938B | Android 16, 1080×2340, releaseVerification | Partial manual smoke passed |
| API 36 emulator | Android 16/API 36 Google Play ARM64, releaseVerification | Recovered; Home and system-scanner entry passed; 98/98 debug connected tests passed |
| API 31 emulator | Android 12/API 31 Google APIs ARM64, 2 GB RAM/two cores at runtime, releaseVerification | Established; Home passed; 98/98 debug connected tests passed |
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

## Emulator recovery and API compatibility increment

- The host supports Android Emulator acceleration. A recovered Android 16/API 36 Google Play ARM64
  profile and a newly created Android 12/API 31 Google APIs ARM64 profile both cold-booted, reached
  ADB, and remained running. The API 31 profile was run at 2 GB RAM and two cores; it is an
  emulator resource profile, not a physical low-memory-device claim.
- The local minified, debug-signed `releaseVerification` artifact installed and launched on both
  emulators. Both Home screens displayed `v0.8.0-dev (8) · releaseVerification`; package metadata
  reported `minSdk 26`, `targetSdk 36`, and no `INTERNET` permission.
- API 36 opened ML Kit Document Scanner with capture and gallery-import controls. No scanner,
  ML Kit initialization, or PageHarbor crash was observed. Its headless camera surface did not
  settle sufficiently for deterministic coordinate/hierarchy automation, so this is not a claim
  that an emulator gallery import completed.
- The full debug connected suite passed on both targets: API 31: 98 total, 98 completed,
  0 failures, 0 errors, 0 skipped (158.237 s); API 36: 98 total, 98 completed, 0 failures,
  0 errors, 0 skipped (136.422 s). This includes deterministic OCR-engine, searchable-PDF,
  FileProvider share-intent, lifecycle/session-reset, and 200% font reachability coverage.
- No PageHarbor defect was reproduced. The original API 36 failure was local emulator process
  handling; the API 31 image first landed under the command-line-tools SDK root rather than the
  emulator SDK root, then was installed into the configured SDK root and booted normally.

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

No PageHarbor defect was found in the executed flow. The original API 36 emulator launch failure
was recovered and is not evidence of API 36 incompatibility. The unexecuted physical lower-memory,
tablet, lower-API, five-session, multipage OCR, full emulator gallery-to-SAF/searchable-PDF/share,
and active-operation interruption checks remain beta gaps.
