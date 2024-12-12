package com.ocean.scdemo.stream;

import org.springframework.stereotype.Service;

@Service
public class StreamService {

    public static void main(String[] args) {

        CustomPublisher customPublisher = new CustomPublisher();
        customPublisher.subscribe(new CustomSubstriber());
    }

}
