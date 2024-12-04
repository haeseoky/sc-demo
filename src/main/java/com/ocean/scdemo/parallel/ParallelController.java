package com.ocean.scdemo.parallel;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parallel")
@RequiredArgsConstructor
public class ParallelController {
    private final ParallelService parallelService;

    @GetMapping("/save")
    public String save() {
        List<TestData> testDataList = IntStream.range(0, 100)
            .mapToObj(i -> new TestData(String.valueOf(i)))
            .toList();

        return parallelService.save(testDataList);
    }
}
