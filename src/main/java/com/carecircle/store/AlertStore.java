package com.carecircle.store;

import com.carecircle.model.Alert;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// שכבת אחסון להתראות — HashMap בזיכרון
// כשתעבור ל-DynamoDB (שלב 3 מחדש), רק הפנים משתנות
public class AlertStore {

    // ConcurrentHashMap כי כמה threads (Worker + Subscriber) יכולים לכתוב בו-זמנית
    private final Map<String, Alert> alerts = new ConcurrentHashMap<>();

    public void save(Alert alert) {
        alerts.put(alert.getId(), alert);
    }

    public Alert findById(String id) {
        return alerts.get(id);
    }

    // מחזיר את כל ההתראות לאדם מסוים
    public List<Alert> findByPersonId(String personId) {
        List<Alert> result = new ArrayList<>();
        for (Alert alert : alerts.values()) {
            if (personId.equals(alert.getPersonId())) {
                result.add(alert);
            }
        }
        return result;
    }
}