package com.ocean.scdemo.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BigBannerReadData extends ReadData{
    private String bannerCode;
    private String memberId;


    public BigBannerReadData(String bannerCode, boolean readYn, String readAt) {
        super(readYn, readAt);
        this.bannerCode = bannerCode;


    }
}
