package com.ocean.scdemo.sample.domain.repository;

import com.ocean.scdemo.sample.domain.Person;

public interface PersonRepository {
    Person findPerson(Long id);
    Person findPerson(String email);
    void savePerson(Person person);
    void deletePerson(Long id);

}
