package com.carecircle.model;

import java.util.UUID;

public class Person {
    private final String id;
    private final String name;
    private final String circleId;

    public Person(String name, String circleId){
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.circleId = circleId;
    }

    public String getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getCircleId(){
        return circleId;
    }
}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
