package com.ocean.scdemo.sample.infrastructure.model.response;

public record SampleResponse(
    String name,
    String description
) {

    public static SampleResponse createEmpty() {
        return new SampleResponse("", "");
    }
}
