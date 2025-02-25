package com.ocean.scdemo.resttemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootTest
@Slf4j
public class RestTemplateControllerTest{

    @SneakyThrows
    @Test
    public void testGet(){

        List<MultipartFile> file = List.of(
            new MockMultipartFile("data", "test.txt", MediaType.TEXT_PLAIN_VALUE, "Hello, World!".getBytes()),
            new MockMultipartFile("data", "test1.png", MediaType.IMAGE_PNG_VALUE, "Hello1111, World!".getBytes())
        );

        String serverUrl = "http://localhost:8080/file/saves";

//        MultipartFile file = new MockMultipartFile("data", "test.txt", MediaType.TEXT_PLAIN_VALUE, "Hello, World!".getBytes());
        // HttpHeaders 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // MultiValueMap 생성 및 파일 추가
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        file.forEach(f -> {
            body.add("data", f.getResource());
        });

        // HttpEntity 생성
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//        HttpEntity<List<MultipartFile>> requestEntity = new HttpEntity<>(file, headers);

        // RestTemplate 생성 및 요청 전송
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(serverUrl, requestEntity, String.class);

        String body1 = response.getBody();

        log.info("response: {}", body1);


    }


    @SneakyThrows
    @Test
    void test(){

        File file = new File("/Users/haeseoky/work/study/project/spring/react/img.png");


        byte[] bytes = Files.readAllBytes(file.toPath());

        int length1 = bytes.length;
        log.info("origin file: {}", length1);
        String s = Base64.getEncoder().encodeToString(bytes);

        int length = s.getBytes().length;
        log.info("convert file: {}", length);

        log.info("base64 encode: {}", s);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<Base64Request> request = new HttpEntity<>(new Base64Request(s), headers);
        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity("http://localhost:8080/file/base64EncodingFile", request, String.class);
    }


    @Test
    void upload() throws IOException {

        WebClient webClient = WebClient.builder().build();

        MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();

        // file객체 생성
        File file = new File("123.png");
        data.add("data", file);


        String block = webClient.post().uri("http://localhost:8080/api/upload").bodyValue(data).retrieve().bodyToMono(String.class).block();

        log.info("upload response: {}", block);

    }

    @Test
    void create(){
        File file = new File("img.png");
        log.info("{}", file.getAbsoluteFile());
    }



    @Test
    void test123(){
        WebClient webClient = WebClient.builder().build();

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        ByteArrayResource part1 = new ByteArrayResource(new byte[1]) {
            @Override
            public String getFilename() {
                return "test.png";
            }
        };
        ByteArrayResource part2 = new ByteArrayResource(new byte[2]) {
            @Override
            public String getFilename() {
                return "test.png";
            }
        };

        builder.part("data", part1);
        builder.part("data", part2);



        Mono<String> response = webClient.post()
            .uri("http://localhost:8080/api/upload")
            .body(BodyInserters.fromMultipartData(builder.build()))
            .retrieve()
            .bodyToMono(String.class);

        String block = response.block();
    }
}
