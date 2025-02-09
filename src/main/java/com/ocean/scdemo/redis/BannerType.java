package com.ocean.scdemo.redis;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BannerType {
    BIG_BANNER("big_banner", "빅배너"),
    SMALL_BANNER("small_banner", "스몰배너"),
    ;

    private final String code;
    private final String desc;

    public static BannerType findByCode(String code) {
        return Arrays.stream(BannerType.values())
            .filter(v -> v.getCode().equals(code))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException("Invalid BannerType code: " + code));
    }
}
