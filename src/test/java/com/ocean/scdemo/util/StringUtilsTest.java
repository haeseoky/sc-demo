package com.ocean.scdemo.util;

import static org.junit.jupiter.api.Assertions.*;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class StringUtilsTest {

    @Test
    void removeUnreconizedChar() {

        // nbsp show in code
//        String str = "abc123 !@# : : ";
        String str = "abc123!@#\uD83D\uDE00�: :� ����������";

        log.info("str: {}", str);
        String result = StringUtils.removeUnreconizedChar(str);

        log.info("result: {}", result);
        assertEquals("abc123!@#::", result);

    }
}