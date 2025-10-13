package com.ocean.scdemo.redis;

import lombok.Getter;

@Getter
public class BlockButtonVO { // Complexity is 3 Everything is co

    private BlockButtonType type;
    private String label;
    private BlockButtonAction action;

    private BlockButtonVO() {
        this.type = BlockButtonType.DEFAULT;
        this.label = BlockButtonType.DEFAULT.getDesc();
        this.action = BlockButtonAction.CLOSE;
    }

    public BlockButtonVO(BlockButtonType type, String label, BlockButtonAction action) {
        this.type = type;
        this.label = label;
        this.action = action;
    }

    public static BlockButtonVO empty() {
        return new BlockButtonVO();
    }
}
