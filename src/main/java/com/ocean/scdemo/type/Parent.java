package com.ocean.scdemo.type;

import java.util.List;
import lombok.Getter;

@Getter
public class Parent {

    private final String familyName;

    private final List<? extends Children> childrenList;

    public Parent() {
        this.familyName = "yun";
        this.childrenList = List.of();
    }

    public Parent(String familyName, List<? extends Children> childrenList) {
        this.familyName = familyName;
        this.childrenList = childrenList;
    }

    public Parent(List<? extends Children> childrenList) {
        this.childrenList = childrenList;
        this.familyName = "yun";
    }
}
