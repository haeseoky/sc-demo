package com.ocean.scdemo.type;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TypeService {

    public Parent getParent() {
        return new Parent(
            "yun family",
            List.of(
                new Boy(9, "new car"),
                new Girl(10, "new barbie")
            )
        );
    }

}
