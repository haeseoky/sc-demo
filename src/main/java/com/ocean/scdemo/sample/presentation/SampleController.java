package com.ocean.scdemo.sample.presentation;

import com.ocean.scdemo.sample.application.SampleCommand;
import com.ocean.scdemo.sample.application.SampleQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sample/v1")
public class SampleController {
    private final SampleCommand sampleCommand;
    private final SampleQuery sampleQuery;

    public SampleController(SampleCommand sampleCommand, SampleQuery sampleQuery) {
        this.sampleCommand = sampleCommand;
        this.sampleQuery = sampleQuery;
    }

    @GetMapping("/sample")
    public String getSample() {
        return "Sample API";
    }

}
