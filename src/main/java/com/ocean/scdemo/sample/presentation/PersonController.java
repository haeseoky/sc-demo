package com.ocean.scdemo.sample.presentation;

import com.ocean.scdemo.sample.application.PersonCommand;
import com.ocean.scdemo.sample.application.PersonQuery;
import com.ocean.scdemo.sample.presentation.model.ResPersonDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/person/v1")
public class PersonController {
    private final PersonCommand personCommand;
    private final PersonQuery personQuery;

    public PersonController(PersonCommand personCommand, PersonQuery personQuery) {
        this.personCommand = personCommand;
        this.personQuery = personQuery;
    }

    @GetMapping("/sample")
    public String getSample() {
        return "Sample API";
    }

    @GetMapping("/family")
    public ResPersonDto getFamily() {
        return ResPersonDto.fromDomain(personQuery.getFamily());
    }

}
