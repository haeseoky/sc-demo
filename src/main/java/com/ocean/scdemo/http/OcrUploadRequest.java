package com.ocean.scdemo.http;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
public class OcrUploadRequest {
    private List<MultipartFile> data;
}
