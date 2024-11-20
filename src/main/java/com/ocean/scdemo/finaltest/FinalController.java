package com.ocean.scdemo.finaltest;

import com.ocean.scdemo.finaltest.model.FinalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/final")
@RequiredArgsConstructor
public class FinalController {

    private final FinalService finalService;


    @PostMapping("/test")
    public FinalResponse test(
        @RequestBody FinalRequest finalRequest
    ) {
        log.info("Test method called");
        FinalResponse call = finalService.call(finalRequest.toFinalClass());
        return call;
    }
}
