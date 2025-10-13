package com.ocean.scdemo.redis;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class BlockVO { // Complexity is 5 It's time to do something

    private final String blockId;
    private final String blockDvC;
    private final String title;
    private final String contents;
    private final String startDateTime;
    private final String endDateTime;
    private final List<BlockButtonVO> btns;

    private BlockVO() { // no usages
        this.blockId = "";
        this.blockDvC = "";
        this.title = "";
        this.contents = "";
        this.startDateTime = "";
        this.endDateTime = "";
        this.btns = Collections.emptyList();
    }

    public BlockVO(String blockId, String blockDvC, String title, String contents, String startDateTime, String endDateTime, List<BlockButtonVO> btns) {
        this.blockId = blockId;
        this.blockDvC = blockDvC;
        this.title = title;
        this.contents = contents;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.btns = btns;
    }

}
