package com.ocean.scdemo.sample.infrastructure.jpa;

import com.ocean.scdemo.sample.infrastructure.model.entity.PersonEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonJpaRepository extends JpaRepository<PersonEntity, Long> {
    Optional<PersonEntity> findByEmail(String email);
    List<PersonEntity> findByName(String name);

}
