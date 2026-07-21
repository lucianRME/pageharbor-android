# Lifecycle and Failure Validation

Status: `v0.6.0-dev` in progress.

## Ownership model

Searchable-PDF preparation, destination writing, and prepared temporary output are owned by the current `MainActivity` instance. `SearchablePdfOperationTracker` issues an in-memory operation token for each preparation. Progress applies only while the token is current and preparation has not completed. Exactly one completion may publish a prepared export; a superseded completion discards only its own prepared output, while a duplicate completion does nothing. It therefore cannot replace or delete a newer prepared export.

On scan replacement, Discard, or Activity destruction, the activity invalidates the token, cancels its searchable-PDF job, and discards only its currently owned prepared output. A later picker result with no owned prepared export is ignored. The coordinator uses a distinct private random temporary file for each prepared export and cleanup is idempotent.

## Cancellation and picker behavior

Cancellation propagates through PDF generation and streamed destination copy. It is not mapped to a generic provider or generation failure. Picker cancellation discards the prepared output and leaves the Scan Result retryable. A stale Activity Result callback has no owned prepared export and is ignored safely; it cannot initiate a write for a newer export.

No active-operation recovery is attempted after process death or Activity recreation. On recreation, stable scan/OCR state remains available in memory, while active work is cleaned up and the user can retry from the restored Scan Result. Process death starts a new active scan session.

## Deterministic coverage

`SearchablePdfOperationTrackerTest` verifies active progress, token supersession, single completion claims, duplicate completion rejection, and invalidation. The Activity uses those claims before publishing or discarding a prepared export, so old progress or completion cannot overwrite a new operation.

`LocalSearchablePdfExportCoordinatorTest` uses in-memory output streams at the SAF boundary. It verifies cleanup and safe error categories for a null output stream, provider-open exception, immediate write failure, mid-copy failure, flush failure, close failure, missing prepared output, and copy cancellation. In every tested failure path the private prepared PDF is deleted and the source scan remains untouched. A simulated private-cache deletion refusal is also non-crashing and never reports success; cleanup remains explicitly best-effort in that exceptional filesystem condition. Generator tests cover first- and later-page JPEG failures, generation cancellation, and partial-output cleanup. Repeated discard and cleanup of an already missing output are harmless.

`MainActivityLifecycleTest` uses `ActivityScenario` to recreate a deterministic completed Scan Result, completed OCR Result, and controlled active searchable-PDF state without arbitrary sleeps. It confirms that scan/OCR state remains available after recreation, active searchable-PDF state resets to retryable idle, Discard clears the retained session, and a replacement scan replaces the old state. `PageHarborSessionViewModelTest` covers the same stable-state boundary directly. Existing token and coordinator tests cover stale completion rejection and prepared-output cleanup beneath that boundary.

## Remaining validation

## Samsung manual smoke

Fresh independent one-page scanner sessions on Samsung SM-S938B / Android 16 confirmed that searchable-PDF preparation opens DocumentsUI with the category-safe `document.pdf` title for an unclassified scan. Picker cancellation returns to Scan Result with the accessible product-facing cancellation feedback and permits a successful retry. A manually confirmed retry returned the accessible success feedback. Backgrounding and foregrounding PageHarbor preserved the active Scan Result without a crash or stale feedback.

The prior rotation defect is fixed in production: an Activity-scoped in-memory session ViewModel retains the active Scan Result screen, scan summary, ordered scanner URIs, and completed OCR result across configuration changes. Active OCR, searchable-PDF preparation/write, SAF picker ownership, progress, and transient success/error/cancellation feedback are cancelled or reset to idle; they are never resumed automatically. Prepared private searchable-PDF output is discarded when its original Activity loses ownership. No process-death recovery or persistent document storage is supported.

After the fix, a Samsung manual rotation sequence passed: a fresh Scan Result survived portrait-to-landscape-to-portrait rotation with its actions available; searchable-PDF picker cancellation returned to Scan Result; rotation before retry did not cause a duplicate picker or false error; and a retry saved successfully. Completed OCR Result also survived rotation, Back returned to Scan Result, Discard returned Home, and a fresh scan showed no old scan/OCR feedback. No crash or permission prompt occurred.

Physical third-party provider behavior and a physical mid-copy provider failure remain unvalidated. No physical process-death recovery is supported or planned.
