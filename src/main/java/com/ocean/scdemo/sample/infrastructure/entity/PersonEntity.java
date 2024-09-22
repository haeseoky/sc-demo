package com.ocean.scdemo.sample.infrastructure.entity;

import com.ocean.scdemo.sample.domain.Gender;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "person",
    indexes = {
        @Index(name = "idx_person_email", columnList = "email"),
        @Index(name = "idx_person_phone", columnList = "phone"),
        @Index(name = "idx_person_identity", columnList = "identity"),
        @Index(name = "idx_person_name", columnList = "name")
    }
)
public class PersonEntity {
    private String name;
    private String identity;
    private String address;
    private String email;
    private String phone;
    private Gender gender;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
