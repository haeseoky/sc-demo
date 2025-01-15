package com.ocean.scdemo.banner;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerService {


    public List<Banner> getBanners() {
        log.info("getBanners");

        List<Banner> banners = List.of(
            new Banner("1", "banner1", "kind1", "description1", "imageUrl1", "linkUrl1", null, null, 1),
            new Banner("2", "banner2", "kind2", "description2", "imageUrl2", "linkUrl2", null, null, 2),
            new Banner("3", "banner3", "kind3", "description3", "imageUrl3", "linkUrl3", null, null, 3),
            new Banner("4", "banner4", "kind4", "description4", "imageUrl4", "linkUrl4", null, null, 4),
            new Banner("5", "banner5", "kind5", "description5", "imageUrl5", "linkUrl5", null, null, 5)
        );


        // banner kind
        // 1: home
        // 2: product
        // 3: event
        // 4: notice
        // 5: etc
        // each kind has condition exposed to user
        // 1: home - no condition
        // 2: product - product is purchased
        // 3: event - event is participated
        // 4: notice - notice is read
        // 5: etc - no condition

        // make each condition bean


        return banners;
    }

}
