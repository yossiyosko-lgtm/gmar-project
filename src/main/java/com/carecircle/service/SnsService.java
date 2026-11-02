package com.carecircle.service;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import java.net.URI;

// אחראי על פרסום התראות ל-SNS Topics
public class SnsService {

    private final SnsClient client;
    private final String gentleTopicArn;
    private final String urgentTopicArn;

    public SnsService(String gentleTopicArn, String urgentTopicArn) {
        this.gentleTopicArn = gentleTopicArn;
        this.urgentTopicArn = urgentTopicArn;
        String endpoint = System.getenv().getOrDefault("LOCALSTACK_ENDPOINT", "http://localhost:4566");
        this.client = SnsClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();
    }

    // פרסום התראה עדינה (z > 2)
    public void publishGentle(String message) {
        PublishRequest request = PublishRequest.builder()
                .topicArn(this.gentleTopicArn)
                .message(message)
                .subject("Care Circle - Gentle Alert")
                .build();
        client.publish(request);
    }

    // פרסום התראה דחופה (z > 3)
    public void publishUrgent(String message) {
        PublishRequest request = PublishRequest.builder()
                .topicArn(this.urgentTopicArn)
                .message(message)
                .subject("Care Circle - URGENT Alert")
                .build();
        client.publish(request);
    }
}