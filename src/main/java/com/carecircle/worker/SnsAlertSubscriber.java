package com.carecircle.worker;

import com.carecircle.model.Alert;
import com.carecircle.store.AlertStore;
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

// Thread שקורא מ-alerts-queue (שה-SNS מאכלס) ושומר Alerts
public class SnsAlertSubscriber extends Thread {

    private final SqsClient sqsClient;
    private final String alertsQueueUrl;
    private final AlertStore alertStore;

    public SnsAlertSubscriber(String alertsQueueUrl, AlertStore alertStore) {
        this.alertsQueueUrl = alertsQueueUrl;
        this.alertStore = alertStore;
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
        while (true) {
            try {
                ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                        .queueUrl(this.alertsQueueUrl)
                        .maxNumberOfMessages(5)
                        .waitTimeSeconds(10)
                        .build();

                List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

                for (Message message : messages) {
                    processAlert(message);
                }

            } catch (Exception e) {
                System.err.println("SnsAlertSubscriber error: " + e.getMessage());
            }
        }
    }

    private void processAlert(Message message) {
        try {
            Gson gson = new Gson();
            // SNS עוטף את ההודעה ב-JSON נוסף — צריך לקלף שכבה
            JsonObject snsWrapper = gson.fromJson(message.body(), JsonObject.class);
            String innerMessage = snsWrapper.get("Message").getAsString();
            JsonObject alertData = gson.fromJson(innerMessage, JsonObject.class);

            String personId = alertData.get("personId").getAsString();
            String type = alertData.get("type").getAsString();
            String severity = alertData.get("severity").getAsString();

            // שמירת ה-Alert ב-AlertStore
            alertStore.save(new Alert(personId, type, severity));
            System.out.println("Alert saved: " + severity + " for " + personId);

            // מחיקה מהתור
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(this.alertsQueueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);

        } catch (Exception e) {
            System.err.println("processAlert error: " + e.getMessage());
        }
    }
}