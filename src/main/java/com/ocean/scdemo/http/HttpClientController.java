package com.ocean.scdemo.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(value = "/api")
public class HttpClientController {

//    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public String upload(@RequestHeader HttpHeaders headers, @RequestPart("data")List<MultipartFile> files){
//
//        log.info("headers: {}", headers);
//
//        for (MultipartFile file : files) {
//            log.info("File name: {}", file.getOriginalFilename());
//            log.info("File size: {}", file.getSize());
//            log.info("File content type: {}", file.getContentType());
//        }
//
//        return "";
//    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestHeader HttpHeaders headers, OcrUploadRequest files) {

        log.info("headers: {}", headers);

        for (MultipartFile file : files.getData()) {
            log.info("File name: {}", file.getOriginalFilename());
            log.info("File size: {}", file.getSize());
            log.info("File content type: {}", file.getContentType());
        }

        return "";
    }

    @GetMapping
    public Boolean test() {
        WebClient build = WebClient.builder()
            .baseUrl("http://localhost:9900").build();
        Mono<String> stringMono = build.get().uri("/orders/{orderId}", "1")
            .retrieve()
            .bodyToMono(String.class);

        stringMono.subscribe(System.out::println);

        return true;
    }


}
