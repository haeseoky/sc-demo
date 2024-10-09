package com.ocean.scdemo.sample.infrastructure.entity;

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
public record PersonEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id,

    @Column(name = "name", nullable = false)
    String name,
    @Column(name = "identity", nullable = false)
    String identity,
    @Column(name = "birth", nullable = false)
    LocalDate birth,
    @Column(name = "address", nullable = false, length = 1000)
    String address,
    @Column(name = "email", nullable = false)
    String email,
    @Column(name = "phone", nullable = false, length = 20)
    String phone,

    @Column(name = "gender", nullable = false)
    @Convert(converter = GenderConverter.class)
    Gender gender
) {

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
