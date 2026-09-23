package com.carecircle.store;

import com.carecircle.model.CheckIn;
import com.carecircle.model.Person;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import java.net.URI;
import java.util.HashMap;

public class CheckInStore {
    
    private final DynamoDbClient client;
    private static final String TABLE_NAME = "CheckIns";

     public CheckInStore(){
        this.client = DynamoDbClient.builder()
        .endpointOverride(URI.create("http://localhost:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test", "test")))
        .build();
    }
    
    public void save(CheckIn checkIn){
       
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("id", AttributeValue.builder().s(checkIn.getId()).build());
        item.put("personId", AttributeValue.builder().s(checkIn.getPersonId()).build());
        item.put("type", AttributeValue.builder().s(checkIn.getType()).build());
        item.put("value", AttributeValue.builder().n(String.valueOf(checkIn.getValue())).build());
        item.put("timestamp", AttributeValue.builder().n(String.valueOf(checkIn.getTimestamp())).build());

        PutItemRequest request = PutItemRequest.builder()
        .tableName(TABLE_NAME)
        .item(item)
        .build();

        client.putItem(request);
    }

    public CheckIn findById(String id){
         Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        GetItemRequest request = GetItemRequest.builder()
        .tableName(TABLE_NAME)
        .key(key)
        .build();

        GetItemResponse response = client.getItem(request);

        if (!response.hasItem()){
            return null;
        }else{
            String personId = response.item().get("personId").s();
            String type = response.item().get("type").s();
            Double value = Double.parseDouble(response.item().get("value").n());
            long timestamp = Long.parseLong(response.item().get("timestamp").n());

            return new CheckIn (id, personId, type, value, timestamp);
        }
    }

}
