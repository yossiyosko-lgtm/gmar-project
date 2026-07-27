package com.carecircle.store;

import com.carecircle.model.Person;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeopleStore {

    private final Map<String, Person> people = new ConcurrentHashMap<>();

    public void save(Person person){
    people.put(person.getId(), person);
    }
}
