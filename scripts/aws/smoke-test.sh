#!/usr/bin/env bash
set -euo pipefail
REGION="${AWS_REGION:-us-east-1}"
PROFILE_ARGS=()
if [[ -n "${AWS_PROFILE:-}" ]]; then PROFILE_ARGS+=(--profile "$AWS_PROFILE"); fi
AWS=(aws "${PROFILE_ARGS[@]}" --region "$REGION")
QUEUE_URL="${AWS_RECONCILIATION_QUEUE_URL:?AWS_RECONCILIATION_QUEUE_URL is required}"
TOPIC_ARN="${AWS_PAYMENT_TOPIC_ARN:?AWS_PAYMENT_TOPIC_ARN is required}"
MESSAGE="rms-dev-smoke-$(date +%s)"
"${AWS[@]}" sns publish --topic-arn "$TOPIC_ARN" --message "$MESSAGE" >/dev/null
for attempt in 1 2 3 4 5; do
  body=$("${AWS[@]}" sqs receive-message --queue-url "$QUEUE_URL" --wait-time-seconds 2 --max-number-of-messages 1 --query 'Messages[0].Body' --output text 2>/dev/null || true)
  if [[ "$body" == *"$MESSAGE"* ]]; then echo "SNS-to-SQS delivery verified"; exit 0; fi
done
echo "Timed out after five bounded receive attempts" >&2
exit 1
