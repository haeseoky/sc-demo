package com.ocean.scdemo.util;

import java.nio.charset.Charset;
import org.springframework.stereotype.Component;

@Component
public class StringUtils {
    public static String removeUnreconizedChar(String str) {
        Charset charset = Charset.forName("EUC-KR");
        return str.chars()
            .filter(c -> charset.newEncoder().canEncode((char) c))
            .collect(
                StringBuilder::new,
                StringBuilder::appendCodePoint,
                StringBuilder::append
            )
            .toString();
    }
}
