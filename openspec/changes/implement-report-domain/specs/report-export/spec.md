## ADDED Requirements

### Requirement: System exports a report to PDF
The system MUST generate a PDF document for a report and persist it as a `ReportExport` (format PDF, stored content, file name and content type) linked to the report.

#### Scenario: Export a generated report to PDF
- **WHEN** a report in status GENERATED is exported
- **THEN** the system produces a PDF `ReportExport` and links it to the report

#### Scenario: Download returns the stored PDF bytes
- **WHEN** a user requests the download of an existing export
- **THEN** the system returns the PDF bytes with the correct `Content-Type` (application/pdf) and `Content-Disposition` file name

#### Scenario: Export of a failed report is rejected
- **WHEN** a user requests the export of a report in status FAILED or PENDING
- **THEN** the system does not produce a PDF and returns a 409 Conflict error

### Requirement: Report entity tracks lifecycle and links to exports
The system MUST persist report metadata (`report_type`, optional `athlete_id`, optional `category`, year/month filters, `title`, `status`) and expose the list of exports associated with a report. Status transitions PENDING → GENERATED or FAILED; soft-deleted reports are excluded from listings.

#### Scenario: Report metadata and status
- **WHEN** a report is created and then generated successfully
- **THEN** its status transitions from PENDING to GENERATED and its exports list includes the generated PDF

#### Scenario: Report marked FAILED on error
- **WHEN** report generation fails (e.g., missing reference triple)
- **THEN** the report status is set to FAILED with an error message, and no export is created

#### Scenario: Soft-deleted report is excluded from listings
- **WHEN** a report is soft-deleted
- **THEN** it no longer appears in list queries, but its stored `ReportExport` (kept for audit) remains linked

### Requirement: Export does not mutate source modules
The system MUST generate PDF exports without persisting or mutating any row of the athlete, training or checkup modules.

#### Scenario: Export is read-only against source modules
- **WHEN** a PDF export is generated
- **THEN** no source-module row is persisted or mutated during that request