package com.carecircle.model;

import java.util.UUID;

/**
 * CheckIn - "דיווח" בודד שאדם שולח למערכת.
 * זה ה"אבן הבניין" שממנה בהמשך (שלב 4) נחשב z-score ונזהה חריגות.
 */
public class CheckIn {

    private final String id;
    private final String personId;   // מקשר את הדיווח הזה לאדם ספציפי (Person.getId())
    private final String type;       // סוג הדיווח: wake_time / medication / memory_quiz
    private final double value;      // הערך המספרי של הדיווח
    private final long timestamp;    // מתי הדיווח נשלח, epoch millis

    public CheckIn(String personId, String type, double value) {
        this.id = UUID.randomUUID().toString();
        this.personId = personId;
        this.type = type;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getPersonId() { return personId; }
    public String getType() { return type; }
    public double getValue() { return value; }
    public long getTimestamp() { return timestamp; }
}