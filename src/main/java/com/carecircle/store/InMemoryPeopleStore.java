package com.carecircle.store;

import com.carecircle.model.Person;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * גרסת זיכרון של PeopleStore - לטסטים, בלי צורך ב-LocalStack.
 */
public class InMemoryPeopleStore extends PeopleStore {

    private final Map<String, Person> people = new ConcurrentHashMap<>();

    @Override
    public void save(Person person) {
        people.put(person.getId(), person);
    }

    @Override
    public Person findById(String id) {
        return people.get(id);
    }
}