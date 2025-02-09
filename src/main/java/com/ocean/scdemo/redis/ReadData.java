package com.ocean.scdemo.redis;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Getter;

@Getter
public abstract class ReadData {
    private final boolean readYn;
    private final String readAt;

    public ReadData(boolean readYn, String readAt) {
        this.readYn = readYn;
        this.readAt = readAt;
    }

    public ReadData() {
        this.readYn = false;
        this.readAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
