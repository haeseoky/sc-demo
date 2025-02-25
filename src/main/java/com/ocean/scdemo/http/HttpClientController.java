package com.ocean.scdemo.http;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(value = "/api")
public class HttpClientController {

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestHeader HttpHeaders headers, @RequestPart("data")List<MultipartFile> files){

        log.info("headers: {}", headers);

        for (MultipartFile file : files) {
            log.info("File name: {}", file.getOriginalFilename());
            log.info("File size: {}", file.getSize());
            log.info("File content type: {}", file.getContentType());
        }

        return "";
    }
}
