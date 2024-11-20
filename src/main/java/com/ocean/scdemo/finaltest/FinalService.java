package com.ocean.scdemo.finaltest;

import com.ocean.scdemo.finaltest.model.FinalClass;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinalService {

    public FinalResponse call(FinalClass finalClass) {

        log.info("Call method called");
        log.info("FinalClass: {}", finalClass);

        return convert(finalClass);
    }

    public FinalResponse convert(final FinalClass finalClass) {
        return FinalResponse.of(finalClass);
    }
}
