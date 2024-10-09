package com.ocean.scdemo.sample.infrastructure.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_class")
public record SchoolClassEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id,
    String name,
    String teacher,
    int maxStudentCount
) {

}
