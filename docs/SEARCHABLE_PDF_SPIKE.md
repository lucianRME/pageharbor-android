# Searchable PDF Technical Spike

Status: `v0.4.0-dev` production PDF-generation foundation. No scan, save, share, JPEG-export, permission, network, or user-interface flow is changed. The generator remains internal and caller-driven; only PdfBox-Android is promoted to the production APK.

## Problem statement

PageHarbor receives a scanner-produced PDF and JPEG page URIs. Its offline OCR result had only page-indexed plain text. A searchable PDF must preserve the scan appearance while adding selectable text that remains local, temporary, and absent from logs.

## PDF source strategy

| Strategy | Fidelity and metadata | Complexity and risk | Outcome |
| --- | --- | --- | --- |
| A. Append to the ML Kit PDF | Keeps the exact scanner PDF page appearance, size, and metadata when the source PDF can be safely preserved. | Requires robust parsing/appending of scanner-owned PDFs, including rotation, compression, encryption, and metadata behavior. | Do not use first. Reconsider only after a representative scanner-PDF compatibility suite. |
| B. Compose from scanner JPEG pages and OCR layout | Embeds the scanner page as the visible background at its native aspect ratio; page size/rotation are PageHarbor-owned. | Requires image/page scale, font embedding, and a private temporary result; scanner-PDF metadata is not automatically retained. | **Recommended.** One controlled image/coordinate system and explicit cleanup. |
| C. Ask an OCR engine to write a PDF | Some native engines can write searchable PDFs. | Couples output format/fidelity to the OCR vendor and adds native/model lifecycle cost. | Reject for MVP. |

For B, make PDF dimensions proportional to the source JPEG. A first version may use one PDF point per image pixel; it must not claim a print size. If print sizing is later needed, consistently use `points = pixels × 72 / targetDpi` for both image and text.

## PDF implementation options

| Candidate | License and maintenance | Capability | Size and risk | Outcome |
| --- | --- | --- | --- | --- |
| Android `PdfDocument` | Platform API, maintained. | Draws arbitrary canvas content but offers no invisible-text mode, embedded Unicode-font control, text-extraction semantics, or existing-PDF editing API. | No dependency, but insufficient evidence of interoperable selectable text. | Insufficient. |
| PdfBox-Android `2.0.27.0` | Apache-2.0 port of PDFBox `2.0.27`; Maven artifact last modified 2023-01-02. | API 19+, pure Java/Android port, multipage pages, image XObjects, embedded `PDType0Font`, affine transforms, and `RenderingMode.NEITHER` invisible text. Call `PDFBoxResourceLoader.init(context)` first. | Main AAR: 3.25 MB. POM declares Bouncy Castle 1.72 transitive artifacts; raw resolved artifacts total about 14.3 MB before R8/APK measurement. No native binary. | **Recommended with constraints.** |
| Apache PDFBox upstream `2.0.27` | Apache-2.0. | Relevant API, but not Android-compatible without the port. | JVM/desktop only. | Not an Android dependency. |
| iText 7 | AGPL or commercial. | Strong PDF/text support. | AGPL conflicts with intended Apache-2.0 distribution absent a commercial licence. | Rejected. |
| OpenPDF | LGPL/MPL dual licence. | PDF writing and font embedding, but Android/invisible-text behavior was not validated. | Requires more licence and Android maintenance review. | Not selected. |

PdfBox-Android’s README specifies version, Apache-2.0 licensing, API 19+, and resource-loader setup. PDFBox documents `RenderingMode.NEITHER` as neither filling nor stroking text. Android `PdfDocument` documents only canvas drawing.

## OCR model changes

The old `OcrPageResult` had `pageIndex`, `text`, and error only; it was insufficient for placement. This spike adds a nullable engine-neutral layout:

```kotlin
OcrPageLayout(imageWidthPx, imageHeightPx, rotationDegrees, lines)
OcrTextLine(text, OcrTextBounds(left, top, right, bottom), confidence?)
```

`MlKitOcrEngine` maps ML Kit line bounds into this model and keeps ML Kit types inside the adapter. The recorded dimensions are the bounded OCR bitmap dimensions, which may differ from the original JPEG. Rotation is explicitly `0` because the current adapter passes an upright decoded bitmap to `InputImage.fromBitmap(bitmap, 0)`.

The production implementation must normalize JPEG EXIF orientation before OCR and emit this upright contract. The PDF writer must never receive ML Kit types, URIs, paths, or raw EXIF values.

## Coordinate-system design

Given upright original image `(Iw, Ih)` pixels, OCR bitmap `(Ow, Oh)` pixels, and PDF page `(Pw, Ph)` points, first scale OCR geometry to the original image:

```text
sxOcr = Iw / Ow;  syOcr = Ih / Oh
imageLeft = ocrLeft × sxOcr; imageTop = ocrTop × syOcr
imageRight = ocrRight × sxOcr; imageBottom = ocrBottom × syOcr
```

Then map Android top-left coordinates to PDF bottom-left coordinates:

```text
sxPdf = Pw / Iw;  syPdf = Ph / Ih
pdfLeft = imageLeft × sxPdf;   pdfRight = imageRight × sxPdf
pdfBottom = Ph - (imageBottom × syPdf)
pdfTop = Ph - (imageTop × syPdf)
```

`mapOcrBoundsToPdf` implements the second stage, clips edge overflow, and skips empty boxes. Independent X/Y scales cover portrait, landscape, and different dimensions. For letterboxing/aspect fit, add the image draw origin `(dx, dy)` and uniform scale before the final conversion.

Normalize rotation before OCR: clockwise 90° maps `(x, y)` to `(Ih - y, x)` and swaps dimensions; 180° to `(Iw - x, Ih - y)`; 270° to `(y, Iw - x)`. Transform every polygon corner, then enclose it. Cropped scanner JPEGs are authoritative; do not reconstruct pre-crop coordinates.

## Text-layer behavior

Write line boxes in OCR reading order, skip blank/empty boxes, embed a Unicode `PDType0Font`, use invisible PDF rendering mode, and horizontally scale a line from its font metrics to its box. Represent multiline output as separate lines. The prototype verifies `ă â î ș ț ä ö ü ß` survives extraction.

Do not use alpha-zero text as the primary mechanism: viewers can optimize or interpret it differently. Invisible rendering mode is explicit. Selection/copy still reflects OCR errors and approximate bounds. Blank pages have no text commands. Tables, columns, handwriting, skewed baselines, and complex scripts remain limitations.

## Prototype result

`SearchablePdfPrototypeTest` ran successfully on a Samsung SM-S938B (Android 16) on 2026-07-20. It creates a deterministic non-sensitive local bitmap, writes a one-page lossless image PDF, embeds the library-provided Liberation Sans font, overlays invisible Romanian/German text, parses exact extracted text, verifies an image XObject, and deletes its private installed-test-target cache PDF in `finally`. Liberation Sans is SIL Open Font License 1.1; its notice must be retained with the shipped dependency.

This is evidence that the PDF model is searchable/copyable and the visible page is an image background. It is not a manual cross-viewer selection result. Before shipping, manually validate search, selection, copy, and visuals in Adobe Acrobat, Google Drive, Samsung/Android, and desktop viewers.

## Performance and privacy

The 400 × 560 fixture is not representative, so it makes no release performance claim. Benchmark 1, 5, and 10 consented pages on low/mid/high-tier devices: wall time, peak heap, output size, cancellation, and cleanup. Process one page at a time; prefer embedding JPEG bytes without bitmap re-encoding after fidelity validation.

The isolated spike had no release APK impact because PdfBox-Android was test-only. This production foundation promotes exactly `com.tom-roush:pdfbox-android:2.0.27.0`; no Bouncy Castle dependency is declared directly, and no networking or native dependency is added. With identical source, the unsigned release APK is **60,277,817 bytes** with the production dependency and **51,864,601 bytes** when it is `compileOnly`: an exact production dependency delta of **+8,413,216 bytes** (8.02 MiB). The release build has minification disabled, so this is the current raw APK impact rather than an App Bundle/download-size estimate. The test APK is not a production-size estimate.

No network access, permission, sensitive logging, persistent document database, or cloud processing was added. The manifest continues explicitly removing `INTERNET` and `ACCESS_NETWORK_STATE`. Production output must be a session-owned private-cache file; delete partial files on failure/cancellation and clean superseded prepared PDFs promptly.

## Implementation plan and recommendation

1. PdfBox-Android `2.0.27.0` is the narrow production dependency because Android `PdfDocument` cannot provide verified invisible text rendering and embedded Unicode-font control.
2. The `SearchablePdfGenerator` foundation accepts ordered caller-owned JPEG streams, engine-neutral OCR results, and a caller-provided temporary output file. It composes image backgrounds, embeds the font, writes `RenderingMode.NEITHER` text, and deletes file outputs after failure or cancellation.
3. Normalize orientation before OCR; retain line reading order and geometry. Add polygons only if boxes prove insufficient.
4. Preserve the original scanner PDF as a fallback until fidelity, multi-viewer, SAF/share, cancellation, and cleanup checks pass.

**Go/no-go: Proceed with constraints.** Use a newly composed JPEG-page PDF plus a PdfBox-Android invisible text layer; do not ship before the stated production validation gates.

## Evidence

- [Android `PdfDocument` API](https://developer.android.com/reference/android/graphics/pdf/PdfDocument)
- [PDFBox invisible text rendering mode](https://pdfbox.apache.org/docs/2.0.0/javadocs/org/apache/pdfbox/pdmodel/graphics/state/RenderingMode.html)
- [PdfBox-Android 2.0.27.0 Maven metadata and dependencies](https://central.sonatype.com/artifact/com.tom-roush/pdfbox-android)
- [PdfBox-Android release artifact size/date](https://repo.maven.apache.org/maven2/com/tom-roush/pdfbox-android/2.0.27.0/)
- [SIL Open Font License 1.1](https://openfontlicense.org/)
