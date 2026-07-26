# PageHarbor Internal Tester Guide

PageHarbor is a privacy-first Android document scanner in development. It scans documents, saves or
shares user-selected output, exports JPEG pages, performs on-device OCR, and can create a searchable
PDF. It has no account, document library, cloud sync, or PageHarbor feedback/analytics service.

Please test normal scans, saves, sharing, OCR, searchable PDFs, cancellation/retry, rotation, and a
second session after Discard. Use the short [beta smoke test](BETA_SMOKE_TEST.md) and include the
version, version code, and build type from About PageHarbor in every report.

Do not use sensitive documents. Do not send document images, OCR text, IDs, invoices, medical
records, full file paths, or unredacted logs. PageHarbor is not expected to support downgrade,
process-death document recovery, an internal document history, cloud sync, or automatic retention.
Report reproducible defects with [BUG_REPORT_TEMPLATE.md](BUG_REPORT_TEMPLATE.md).
