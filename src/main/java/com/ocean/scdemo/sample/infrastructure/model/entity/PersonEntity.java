package com.ocean.scdemo.sample.infrastructure.model.entity;

import com.ocean.scdemo.sample.domain.Gender;
import com.ocean.scdemo.sample.domain.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(
    name = "person",
    indexes = {
        @Index(name = "idx_person_email", columnList = "email", unique = true),
        @Index(name = "idx_person_phone", columnList = "phone"),
        @Index(name = "idx_person_identity", columnList = "identity", unique = true),
        @Index(name = "idx_person_name", columnList = "name")
    }
)
public class PersonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    @Column(name = "name", nullable = false)
    private final String name;
    @Column(name = "identity", nullable = false)
    private final String identity;
    @Column(name = "birth", nullable = false)
    private final LocalDate birth;
    @Column(name = "address", nullable = false, length = 1000)
    private final String address;
    @Column(name = "email", nullable = false)
    private final String email;
    @Column(name = "phone", nullable = false, length = 20)
    private final String phone;

    @Column(name = "gender", nullable = false)
    @Convert(converter = GenderConverter.class)
    private final Gender gender;

    public PersonEntity() {
        this.id = 0L;
        this.name = "name";
        this.identity = "identity";
        this.birth = LocalDate.now();
        this.address = "address";
        this.email = "email";
        this.phone = "phone";
        this.gender = Gender.MAN;
    }

    private PersonEntity(Long id, String name, String identity, LocalDate birth, String address, String email, String phone, Gender gender) {
        this.id = id;
        this.name = name;
        this.identity = identity;
        this.birth = birth;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
    }

    public Person toDomain() {
        return new Person(
            name,
            identity,
            birth,
            address,
            email,
            phone,
            gender
        );
    }

    public static PersonEntity fromDomain(Person person) {
        return new PersonEntity(
            null,
            person.name(),
            person.identity(),
            person.birth(),
            person.address(),
            person.email(),
            person.phone(),
            person.gender()
        );
    }
}
