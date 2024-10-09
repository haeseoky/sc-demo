package com.ocean.scdemo.sample.infrastructure;

import com.ocean.scdemo.sample.domain.Person;
import com.ocean.scdemo.sample.domain.repository.PersonRepository;
import com.ocean.scdemo.sample.infrastructure.entity.PersonEntity;
import com.ocean.scdemo.sample.infrastructure.jpa.PersonJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepositoryImpl implements PersonRepository {
    private final PersonJpaRepository personJpaRepository;

    public PersonRepositoryImpl(PersonJpaRepository personJpaRepository) {
        this.personJpaRepository = personJpaRepository;
    }

    @Override
    public Person findPerson(Long id) {
        return personJpaRepository.findById(id).map(PersonEntity::toDomain).orElse(Person.createEmpty());
    }

    @Override
    public Person findPerson(String email) {
        return personJpaRepository.findByEmail(email).map(PersonEntity::toDomain).orElse(Person.createEmpty());
    }

    @Override
    public List<Person> findPersons(String name) {
        return personJpaRepository.findByName(name).stream().map(PersonEntity::toDomain).toList();
    }


    @Override
    public void savePerson(Person person) {
        personJpaRepository.save(PersonEntity.fromDomain(person));
    }

    @Override
    public void deletePerson(Person person) {
        personJpaRepository.delete(PersonEntity.fromDomain(person));
    }
}
