package com.carecircle.worker;

import com.carecircle.service.SnsService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Thread שרץ ברקע, קורא check-ins מ-SQS ומחשב z-score
public class CheckInWorker extends Thread {

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final SnsService snsService;
    // היסטוריה לכל אדם+סוג דיווח: "personId:type" -> רשימת ערכים
    private final Map<String, List<Double>> history = new ConcurrentHashMap<>();

    public CheckInWorker(String queueUrl, SnsService snsService) {
        this.queueUrl = queueUrl;
        this.snsService = snsService;
        String endpoint = System.getenv().getOrDefault("LOCALSTACK_ENDPOINT", "http://localhost:4566");
        this.sqsClient = SqsClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();
    }

    @Override
    public void run() {
        // לולאה אינסופית — ה-Worker לא עוצר לעולם
        while (true) {
            try {
                // בקשת עד 5 הודעות, ממתין עד 10 שניות אם אין (Long Polling)
                ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                        .queueUrl(this.queueUrl)
                        .maxNumberOfMessages(5)
                        .waitTimeSeconds(10)
                        .build();

                List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

                for (Message message : messages) {
                    processMessage(message);
                }

            } catch (Exception e) {
                // מדפיס שגיאה ומנסה שוב — לא קורס
                System.err.println("Worker error: " + e.getMessage());
            }
        }
    }

    private void processMessage(Message message) {
        try {
            Gson gson = new Gson();
            // פרסור ה-JSON שהגיע מ-SQS
            JsonObject checkIn = gson.fromJson(message.body(), JsonObject.class);
            String personId = checkIn.get("personId").getAsString();
            String type = checkIn.get("type").getAsString();
            double value = checkIn.get("value").getAsDouble();

            // מפתח ייחודי לכל שילוב אדם + סוג דיווח
            String historyKey = personId + ":" + type;
            history.computeIfAbsent(historyKey, k -> new ArrayList<>()).add(value);

            List<Double> values = history.get(historyKey);

            // חישוב z-score רק אם יש לפחות 3 ערכים בהיסטוריה
            if (values.size() >= 3) {
                // ממוצע
                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                // סטיית תקן
                double stdDev = Math.sqrt(values.stream()
                        .mapToDouble(v -> Math.pow(v - mean, 2))
                        .average().orElse(0.0));

                if (stdDev > 0) {
                    double zScore = Math.abs((value - mean) / stdDev);

                    if (zScore > 3) {
                        // חריגה חמורה — פרסום ל-urgent topic
                        String alertJson = String.format(
                            "{\"personId\":\"%s\",\"type\":\"%s\",\"severity\":\"urgent\",\"zScore\":%.2f}",
                            personId, type, zScore);
                        snsService.publishUrgent(alertJson);
                        System.out.println("URGENT published: " + personId + " z=" + zScore);

                    } else if (zScore > 2) {
                        // חריגה עדינה — פרסום ל-gentle topic
                        String alertJson = String.format(
                            "{\"personId\":\"%s\",\"type\":\"%s\",\"severity\":\"gentle\",\"zScore\":%.2f}",
                            personId, type, zScore);
                        snsService.publishGentle(alertJson);
                        System.out.println("GENTLE published: " + personId + " z=" + zScore);
                    }
                }
            }

            // מחיקת ההודעה מהתור אחרי טיפול מוצלח
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(this.queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);

        } catch (Exception e) {
            System.err.println("processMessage error: " + e.getMessage());
        }
    }
}