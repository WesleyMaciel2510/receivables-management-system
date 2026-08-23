# Field-level encryption

Protected client PII (`cpf_cnpj`, `email`, and `phone`) and receivable
`description` values are stored as versioned AES-256-GCM envelopes. The
envelope has the form `rmsfle:v1:<key-version>:<iv>:<ciphertext-with-tag>`;
the IV is random for every write and the authentication tag detects tampering.

Set `APP_ENCRYPTION_SECRET` outside source control. The application derives the
current AES key and a separate HMAC blind-index key from it. `APP_ENCRYPTION_CURRENT_KEY_VERSION`
selects the write key; previous secrets can be supplied as
`APP_ENCRYPTION_LEGACY_SECRETS=1=old-secret` during rotation.

For local Compose, also set `APP_ENCRYPTION_SECRET` before starting the stack;
the value is intentionally required and has no committed fallback.

CPF/CNPJ and email uniqueness use keyed blind indexes. Randomized ciphertext is
never queried directly. Existing rows are read in migration mode and rewritten
with ciphertext and indexes; new writes are encrypted by the JPA converter.

Local Compose provisions `alias/rms-fle` in LocalStack KMS using
`scripts/localstack/10-kms.sh`. Production must set
`APP_ENCRYPTION_KMS_PROVIDER=aws`, use AWS KMS credentials/IAM permissions, and
never use LocalStack endpoints.


## AWS KMS permissions

The production application role is limited to the configured key and requires only
`kms:GenerateDataKey` and `kms:Decrypt`. The key policy should grant those actions
with `Resource` set to the encryption CMK ARN. Do not grant key administration,
`kms:ScheduleKeyDeletion`, `kms:CreateKey`, or wildcard KMS permissions to the
application role. Local development uses `alias/rms-fle` in LocalStack only.

## Rotation and recovery

To rotate, provision the new KMS key/version, set
`APP_ENCRYPTION_CURRENT_KEY_VERSION` and the matching secret, and retain the
previous value in `APP_ENCRYPTION_LEGACY_SECRETS` until the batch migration has
verified every row. New writes use the current version; reads accept retained
legacy versions. Re-encrypt rows in bounded batches, verify decryption and blind
indexes after each batch, then remove the legacy secret only after backup and
restore verification. If verification fails, stop the cutover and restore the
previous secret/version; no plaintext columns are dropped automatically.