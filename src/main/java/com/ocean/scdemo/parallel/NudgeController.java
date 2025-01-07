package com.ocean.scdemo.parallel;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("/nudges")
@RequiredArgsConstructor
public class NudgeController {
    private final NudgeService nudgeService;

    @GetMapping
    public List<String> getNudges(
        @RequestParam(value = "index", required = true) Integer index
    ) {
        return nudgeService.getNudgeList(index);
    }

}
