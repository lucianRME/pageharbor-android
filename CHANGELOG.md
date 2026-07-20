# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Began the `0.3.0-dev` Offline OCR Foundation cycle
- Completed OCR technology research and selected bundled ML Kit Text Recognition v2 Latin for the initial OCR implementation
- Retained Tesseract as a documented future OCR alternative
- Clarified that scanner editing remains provided by ML Kit
- Added a dependency-free OCR engine boundary and in-memory result model
- Bounded OCR JPEG decoding to a 2,800 px long edge and 7 MP per-page bitmap cap
- Added user-initiated offline OCR with an in-memory recognized-text preview and safe partial or empty-result handling
- Added a dedicated in-memory OCR result surface with page-aware formatting and explicit Copy Text
- Refined the app into Home, Scan Result, and OCR Result surfaces with user-facing scan summaries and local operation feedback
- Added a caller-driven local searchable-PDF generation foundation with JPEG image pages and invisible Unicode OCR text; no user-facing export flow changed

## [0.2.0-dev]

### Added

- Initial repository setup
- Added project documentation
- Defined privacy and MVP principles
- Added the initial Android application foundation
- Added the initial Compose placeholder screen
- Added baseline build and test configuration
- Refined the Home screen for the early-development app state
- Added temporary scan action feedback while scanning is not yet implemented
- Added an in-app privacy information dialog
- Documented the MVP product flow
- Defined initial architecture boundaries
- Documented privacy and data lifecycle boundaries
- Added initial architecture decision records
- Added a clearly labelled ML Kit Document Scanner technical spike
- Added the initial branding guide
- Added the initial PageHarbor visual identity
- Added adaptive and monochrome launcher icon resources
- Added branded light and dark themes
- Added Android system splash configuration
- Added initial GitHub and Play Store visual assets
- Added a public roadmap
- Added debug version and build metadata
- Added an About dialog with project attribution
- Added PDF saving through Android's Storage Access Framework for ML Kit scan results
- Added PDF sharing through Android's native share sheet for ML Kit scan results
- Added individual scanned-page export through Android's Storage Access Framework
- Completed physical-device validation on Samsung Android 16

### Fixed

- Restored PDF sharing for scanner results that are readable but not directly grantable to share targets
