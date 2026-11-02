package com.carecircle.service;

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

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

    public void publishGentle(String message) {
        publish(gentleTopicArn, message);
    }

    public void publishUrgent(String message) {
        publish(urgentTopicArn, message);
    }

    private void publish(String topicArn, String message) {
        client.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .build());
    }
}