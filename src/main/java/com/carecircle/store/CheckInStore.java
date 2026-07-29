package com.carecircle.store;

import com.carecircle.model.CheckIn;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CheckInStore {
    
    private final Map<String, CheckIn> checkIns = new ConcurrentHashMap<>();

    public void save(CheckIn checkIn){
    checkIns.put(checkIn.getId(), checkIn);
    }

    public CheckIn findById(String id){
        return checkIns.get(id);
    }

}
