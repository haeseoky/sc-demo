package com.ocean.scdemo.banner;

import java.time.LocalDate;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class Banner {

    private final String bannerId;
    private final String bannerName;
    private final String bannerKind;
    private final String bannerDescription;
    private final String bannerImageUrl;
    private final String bannerLinkUrl;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int displayOrder;

    public Banner(String bannerId, String bannerName, String bannerKind, String bannerDescription, String bannerImageUrl, String bannerLinkUrl, LocalDate startDate, LocalDate endDate, int displayOrder) {
        this.bannerId = bannerId;
        this.bannerName = bannerName;
        this.bannerKind = bannerKind;
        this.bannerDescription = bannerDescription;
        this.bannerImageUrl = bannerImageUrl;
        this.bannerLinkUrl = bannerLinkUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.displayOrder = displayOrder;
    }
}
