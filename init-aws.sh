#!/usr/bin/env bash
set -euo pipefail

AWS="awslocal"
TOPIC="payment-fanout-topic.fifo"
RECONCILIATION_QUEUE="payment-reconciliation-queue.fifo"
ANALYTICS_QUEUE="payment-analytics-queue.fifo"
BUCKET="recebiveis-analytics"

topic_arn="$($AWS sns create-topic --name "$TOPIC" --attributes FifoTopic=true,ContentBasedDeduplication=false --query TopicArn --output text)"
reconciliation_url="$($AWS sqs create-queue --queue-name "$RECONCILIATION_QUEUE" --attributes FifoQueue=true,ContentBasedDeduplication=false,VisibilityTimeout=60 --query QueueUrl --output text)"
analytics_url="$($AWS sqs create-queue --queue-name "$ANALYTICS_QUEUE" --attributes FifoQueue=true,ContentBasedDeduplication=false,VisibilityTimeout=60 --query QueueUrl --output text)"
reconciliation_arn="$($AWS sqs get-queue-attributes --queue-url "$reconciliation_url" --attribute-names QueueArn --query Attributes.QueueArn --output text)"
analytics_arn="$($AWS sqs get-queue-attributes --queue-url "$analytics_url" --attribute-names QueueArn --query Attributes.QueueArn --output text)"

$AWS sns subscribe --topic-arn "$topic_arn" --protocol sqs --notification-endpoint "$reconciliation_arn" >/dev/null
$AWS sns subscribe --topic-arn "$topic_arn" --protocol sqs --notification-endpoint "$analytics_arn" >/dev/null
$AWS s3 mb "s3://$BUCKET" >/dev/null 2>&1 || true

echo "AWS_PAYMENT_TOPIC_ARN=$topic_arn"
echo "AWS_RECONCILIATION_QUEUE_URL=$reconciliation_url"
echo "AWS_ANALYTICS_QUEUE_URL=$analytics_url"
echo "AWS_ANALYTICS_BUCKET=$BUCKET"