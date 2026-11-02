package com.carecircle.service;

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class SqsService {

    private final SqsClient client;
    private final String queueUrl;

    public SqsService(String queueUrl) {
        this.queueUrl = queueUrl;
        // הכתובת נקראת ממשתנה סביבה: בתוך Docker זה http://localstack:4566
        String endpoint = System.getenv().getOrDefault("LOCALSTACK_ENDPOINT", "http://localhost:4566");
        this.client = SqsClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();
    }

    public void sendMessage(String body) {
        client.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .build());
    }
}