package com.ocean.scdemo.parallel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Getter
@Entity
public class TestRdbData {
    @Id
    private final Long id;

    @Column(name = "name", nullable = false)
    private final String name;


    public TestRdbData(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public TestRdbData() {
        this.id = 0L;
        this.name = "default";
    }
}
