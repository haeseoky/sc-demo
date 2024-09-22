package com.ocean.scdemo.junittest.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JunitTestServiceTest {

    @Test
    @DisplayName("Addition Test")
    void add() {
        assertEquals(2, new JunitTestService().add(1, 1));
    }

    @Test
    @DisplayName("Subtraction Test")
    void subtract() {
        assertEquals(0, new JunitTestService().subtract(1, 1));
    }
}