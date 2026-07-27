package com.carecircle.model;
import java.util.UUID;
public class CheckIn {
    private final String id;
    private final String personId;
    private final String type;
    private final double value;
    private final long timestamp;

    public CheckIn(String personalId, String type, double value){
        this.id = UUID.randomUUID().toString();
        this.personId = personalId;
        this.type = type;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId(){
        return id;
    }

    public String getPersonId(){
        return personId;
    }

    public String getType(){
        return type;
    }

    public double getValue(){
        return value;
    }

    public long getTimestamp(){
        return timestamp;
    }

}
