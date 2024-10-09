package com.ocean.scdemo.sample.infrastructure.model.entity;

import com.ocean.scdemo.sample.domain.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender attribute) {
        return attribute.getCode();
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        return Gender.fromCode(dbData);
    }
}
