package com.carecircle.model;
import java.util.UUID;

public class Alert {
    
    private final String id;
    private final String personId;
    private final String type;
    private final String severity;
    private final long timestamp;

    public Alert(String personId, String type, String severity){

        this.personId = personId;

        this.type = type;

        this.severity = severity;

        this.id = UUID.randomUUID().toString();

        this.timestamp = System.currentTimeMillis();
    
    }

    public Alert(String id, String personId, String type, String severity, long timestamp){

        this.id = id;

        this.personId = personId;

        this.type = type;

        this.severity = severity;

        this.timestamp = timestamp;

    }

    public String getId (){
        return this.id;
    }
    
    public String getPersonId(){
        return this.personId;
    }

    public String getType(){
        return this.type;
    }

    public String getSeverity(){
        return this.severity;
    }

    public long getTimestamp(){
        return this.timestamp;
    }


}
