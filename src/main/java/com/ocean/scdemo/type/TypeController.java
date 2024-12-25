package com.ocean.scdemo.type;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/type")
@RequiredArgsConstructor
public class TypeController {
    private final TypeService typeService;

    @GetMapping("/parent")
    public Parent getParent() {
        Parent parent = typeService.getParent();
        return parent;
    }

}
