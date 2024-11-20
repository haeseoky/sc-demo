package com.ocean.scdemo.finaltest;

import com.ocean.scdemo.finaltest.model.FinalClass;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@RequiredArgsConstructor
public class FinalResponse {
    private final String name;
    private final int age;

    public static FinalResponse of(FinalClass finalClass) {
        return new FinalResponse(finalClass.getName(), finalClass.getAge());
    }
}
