package com.carecircle.store;

import com.carecircle.model.Person;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class PeopleStore {

    private final DynamoDbClient client;
    private static final String TABLE_NAME = "People";

    public PeopleStore() {
        // הכתובת נקראת ממשתנה סביבה: בתוך Docker זה http://localstack:4566, מקומית localhost
        String endpoint = System.getenv().getOrDefault("LOCALSTACK_ENDPOINT", "http://localhost:4566");
        this.client = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();
    }

    public void save(Person person) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(person.getId()).build());
        item.put("name", AttributeValue.builder().s(person.getName()).build());
        item.put("circleId", AttributeValue.builder().s(person.getCircleId()).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        client.putItem(request);
    }

    public Person findById(String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        GetItemResponse response = client.getItem(request);

        if (!response.hasItem()) {
            return null;
        }
        String name = response.item().get("name").s();
        String circleId = response.item().get("circleId").s();
        return new Person(id, name, circleId);
    }
}