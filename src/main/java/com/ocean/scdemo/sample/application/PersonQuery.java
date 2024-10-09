package com.ocean.scdemo.sample.application;

import com.ocean.scdemo.sample.domain.Person;
import com.ocean.scdemo.sample.domain.repository.PersonRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PersonQuery {
    private final PersonRepository personRepository;

    public PersonQuery(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public Person getFamily(){
        List<Person> persons = personRepository.findPersons("yun");

        return !persons.isEmpty() ? persons.getFirst() : Person.createEmpty();
    }
}
