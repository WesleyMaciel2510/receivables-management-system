#!/bin/sh
set -eu

ALIAS_NAME="alias/rms-fle"
KEY_ID="$(awslocal kms list-aliases --query "Aliases[?AliasName=='${ALIAS_NAME}'].TargetKeyId | [0]" --output text)"

if [ -z "${KEY_ID}" ] || [ "${KEY_ID}" = "None" ]; then
  KEY_ID="$(awslocal kms create-key --description 'RMS field-level encryption development key' --query 'KeyMetadata.KeyId' --output text)"
  awslocal kms create-alias --alias-name "${ALIAS_NAME}" --target-key-id "${KEY_ID}"
fi

echo "LocalStack KMS ready: ${ALIAS_NAME}"
