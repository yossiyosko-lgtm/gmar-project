package com.carecircle.model;

import java.util.UUID;

/**
 * Person - מייצג בן אחד במעגל המשפחתי (Care Circle).
 * זו "מחלקת נתונים" בלבד (POJO) - אין בה שום לוגיקה עסקית, רק שדות וגישה אליהם.
 */
public class Person {

    private final String id;
    private final String name;
    private final String circleId;

    public Person(String name, String circleId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.circleId = circleId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCircleId() { return circleId; }
}
