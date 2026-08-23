## Purpose

Keep encryption secrets and sensitive configuration values outside source control while allowing controlled environment-specific resolution at application startup.

## ADDED Requirements

### Requirement: Sensitive configuration is externally supplied
The system SHALL obtain the field-encryption master secret and any configuration-decryption secret from environment variables or an external secret provider, never from committed source files.

#### Scenario: Configuration is started in local development
- **WHEN** the application starts through the documented local workflow
- **THEN** the required secret variables are supplied externally and the application does not print their values

#### Scenario: A production secret is absent
- **WHEN** production starts without the required secret material
- **THEN** startup fails closed with a non-sensitive configuration error

### Requirement: Plaintext secrets are not exposed operationally
The system SHALL prevent encryption secrets, decrypted configuration passwords, and protected field values from appearing in application logs, actuator responses, exception payloads, or test diagnostics.

#### Scenario: Startup logging is enabled
- **WHEN** the application initializes encryption and configuration components
- **THEN** logs contain configuration state and safe identifiers only, never secret values or decrypted PII
