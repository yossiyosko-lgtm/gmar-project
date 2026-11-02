package com.carecircle.store;

import com.carecircle.model.CheckIn;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * גרסת זיכרון של CheckInStore - לטסטים, בלי צורך ב-LocalStack.
 */
public class InMemoryCheckInStore extends CheckInStore {

    private final Map<String, CheckIn> checkIns = new ConcurrentHashMap<>();

    @Override
    public void save(CheckIn checkIn) {
        checkIns.put(checkIn.getId(), checkIn);
    }

    @Override
    public CheckIn findById(String id) {
        return checkIns.get(id);
    }
}