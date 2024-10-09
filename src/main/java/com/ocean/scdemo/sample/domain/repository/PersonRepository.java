package com.ocean.scdemo.sample.domain.repository;

import com.ocean.scdemo.sample.domain.Person;
import java.util.List;

public interface PersonRepository {
    Person findPerson(Long id);
    Person findPerson(String email);
    List<Person> findPersons(String name);
    void savePerson(Person person);
    void deletePerson(Person person);

}
