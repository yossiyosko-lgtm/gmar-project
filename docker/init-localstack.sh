#!/bin/bash
# רץ אוטומטית בתוך LocalStack כשהוא מוכן. awslocal = aws CLI מכוון ל-LocalStack.

# טבלאות DynamoDB
awslocal dynamodb create-table --table-name People \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

awslocal dynamodb create-table --table-name CheckIns \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

# תורי SQS
awslocal sqs create-queue --queue-name checkin-queue
awslocal sqs create-queue --queue-name alerts-queue

# נושאי SNS + מנויים שמעבירים התראות לתור alerts-queue
awslocal sns create-topic --name gentle-alerts
awslocal sns create-topic --name urgent-alerts
awslocal sns subscribe --topic-arn arn:aws:sns:us-east-1:000000000000:gentle-alerts \
  --protocol sqs --notification-endpoint arn:aws:sqs:us-east-1:000000000000:alerts-queue
awslocal sns subscribe --topic-arn arn:aws:sns:us-east-1:000000000000:urgent-alerts \
  --protocol sqs --notification-endpoint arn:aws:sqs:us-east-1:000000000000:alerts-queue

echo "DynamoDB tables, SQS queues and SNS topics created"