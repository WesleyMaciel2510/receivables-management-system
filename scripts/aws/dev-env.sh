#!/usr/bin/env bash
set -euo pipefail
REGION="${AWS_REGION:-us-east-1}"
PROFILE_ARGS=()
if [[ -n "${AWS_PROFILE:-}" ]]; then PROFILE_ARGS+=(--profile "$AWS_PROFILE"); fi
AWS=(aws "${PROFILE_ARGS[@]}" --region "$REGION")
require_credentials() { "${AWS[@]}" sts get-caller-identity >/dev/null; }
provision() {
  require_credentials
  local dlq_url queue_url queue_arn topic_arn pool_id client_id dlq_arn redrive
  dlq_url=$("${AWS[@]}" sqs create-queue --queue-name rms-dev-events-dlq --query QueueUrl --output text)
  dlq_arn=$("${AWS[@]}" sqs get-queue-attributes --queue-url "$dlq_url" --attribute-names QueueArn --query Attributes.QueueArn --output text)
  redrive=$(printf '{"deadLetterTargetArn":"%s","maxReceiveCount":"5"}' "$dlq_arn")
  queue_url=$("${AWS[@]}" sqs create-queue --queue-name rms-dev-events --attributes "RedrivePolicy=$redrive,VisibilityTimeout=60,ReceiveMessageWaitTimeSeconds=20" --query QueueUrl --output text)
  queue_arn=$("${AWS[@]}" sqs get-queue-attributes --queue-url "$queue_url" --attribute-names QueueArn --query Attributes.QueueArn --output text)
  topic_arn=$("${AWS[@]}" sns create-topic --name rms-dev-notifications --query TopicArn --output text)
  "${AWS[@]}" sns subscribe --topic-arn "$topic_arn" --protocol sqs --notification-endpoint "$queue_arn" >/dev/null
  pool_id=$("${AWS[@]}" cognito-idp list-user-pools --max-results 60 --query "UserPools[?Name=='rms-dev-users'].Id | [0]" --output text)
  if [[ "$pool_id" == "None" || -z "$pool_id" ]]; then pool_id=$("${AWS[@]}" cognito-idp create-user-pool --pool-name rms-dev-users --query UserPool.Id --output text); fi
  client_id=$("${AWS[@]}" cognito-idp list-user-pool-clients --user-pool-id "$pool_id" --query "UserPoolClients[?ClientName=='rms-dev-client'].ClientId | [0]" --output text)
  if [[ "$client_id" == "None" || -z "$client_id" ]]; then client_id=$("${AWS[@]}" cognito-idp create-user-pool-client --user-pool-id "$pool_id" --client-name rms-dev-client --explicit-auth-flows ALLOW_USER_PASSWORD_AUTH ALLOW_REFRESH_TOKEN_AUTH --query UserPoolClient.ClientId --output text); fi
  printf 'AWS_REGION=%s\nAWS_PAYMENT_TOPIC_ARN=%s\nAWS_RECONCILIATION_QUEUE_URL=%s\nCOGNITO_USER_POOL_ID=%s\nCOGNITO_CLIENT_ID=%s\nCOGNITO_ISSUER_URI=https://cognito-idp.%s.amazonaws.com/%s\n' "$REGION" "$topic_arn" "$queue_url" "$pool_id" "$client_id" "$REGION" "$pool_id"
}
teardown() {
  require_credentials
  local queue_url dlq_url topic_arn pool_id
  queue_url=$("${AWS[@]}" sqs get-queue-url --queue-name rms-dev-events --query QueueUrl --output text 2>/dev/null || true)
  dlq_url=$("${AWS[@]}" sqs get-queue-url --queue-name rms-dev-events-dlq --query QueueUrl --output text 2>/dev/null || true)
  topic_arn=$("${AWS[@]}" sns list-topics --query "Topics[?ends_with(TopicArn, ':rms-dev-notifications')].TopicArn | [0]" --output text)
  pool_id=$("${AWS[@]}" cognito-idp list-user-pools --max-results 60 --query "UserPools[?Name=='rms-dev-users'].Id | [0]" --output text)
  [[ -z "$queue_url" || "$queue_url" == "None" ]] || "${AWS[@]}" sqs delete-queue --queue-url "$queue_url"
  [[ -z "$dlq_url" || "$dlq_url" == "None" ]] || "${AWS[@]}" sqs delete-queue --queue-url "$dlq_url"
  [[ -z "$topic_arn" || "$topic_arn" == "None" ]] || "${AWS[@]}" sns delete-topic --topic-arn "$topic_arn"
  [[ -z "$pool_id" || "$pool_id" == "None" ]] || "${AWS[@]}" cognito-idp delete-user-pool --user-pool-id "$pool_id"
}
case "${1:-}" in provision) provision ;; teardown) teardown ;; *) echo "Usage: $0 {provision|teardown}" >&2; exit 2 ;; esac
