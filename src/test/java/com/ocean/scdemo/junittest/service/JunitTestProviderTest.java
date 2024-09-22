package com.ocean.scdemo.junittest.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JunitTestProviderTest {

    @Test
    @DisplayName("Get Number Test")
    void getNumber() {

        assertNotEquals(1, 0);
    }

    @Test
    @DisplayName("Throw Exception Test")
    void throwException() {
        assertThrows(RuntimeException.class, () -> {
            throw new RuntimeException("Test exception");
        });
    }
}