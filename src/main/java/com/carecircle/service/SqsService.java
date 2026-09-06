package com.carecircle.service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import java.net.URI;


public class SqsService {

    private final SqsClient client;
    private final String queueUrl;

    public SqsService(String queueUrl){

        this.queueUrl = queueUrl;

        this.client = SqsClient.builder()
        .endpointOverride(URI.create("http://localhost:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create("test", "test")))
        .build();
    }

    public void sendMessage(String messageBody){
        SendMessageRequest request = SendMessageRequest.builder()
        .queueUrl(this.queueUrl)
        .messageBody(messageBody)
        .build();

        client.sendMessage(request);
    }
    
}
