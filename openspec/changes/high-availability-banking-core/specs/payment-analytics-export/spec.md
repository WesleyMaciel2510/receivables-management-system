## Purpose

Transform payment events into queryable, compressed columnar data and publish it to partitioned S3-compatible storage for downstream analytics.

## ADDED Requirements

### Requirement: Batch transaction export
The analytics consumer SHALL process payment events individually or in bounded batches and SHALL produce Parquet files containing a stable schema for the transaction fields represented by the events.

#### Scenario: Build a Parquet batch
- **WHEN** a batch of valid payment events is available
- **THEN** the system SHALL create a Snappy-compressed Parquet object containing one record per accepted event and the required transaction columns

### Requirement: Partitioned object storage path
The system SHALL upload generated files to the configured S3-compatible bucket under `year={YYYY}/month={MM}/day={DD}/payments_{timestamp}.parquet`.

#### Scenario: Upload daily analytics data
- **WHEN** a Parquet batch is finalized for a transaction date
- **THEN** the object SHALL be uploaded to the matching year, month, and day partition and SHALL be retrievable from the configured bucket

### Requirement: Analytics delivery safety
The analytics worker SHALL acknowledge source messages only after the Parquet object is durably written, and SHALL make retries safe without silently dropping records.

#### Scenario: Storage upload fails
- **WHEN** S3 upload fails while processing a batch
- **THEN** the source messages SHALL remain retryable and no successful completion acknowledgement SHALL be issued for the failed batch

#### Scenario: Verify exported data
- **WHEN** a generated Parquet object is downloaded
- **THEN** a Parquet-compatible reader SHALL be able to inspect its schema and retrieve the exported payment records
