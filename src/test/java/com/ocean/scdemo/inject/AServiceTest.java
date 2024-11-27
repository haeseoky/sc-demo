package com.ocean.scdemo.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AServiceTest {

    @Mock
    private BService bService;

    private CService cService = mock(CService.class);

    @InjectMocks
    private AService aService = new AService(cService);


    AutoCloseable autoCloseable;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void testDoSomething() {

        given(bService.doSomething()).willReturn("AService.doSomething");
        given(cService.doSomething()).willReturn("CService.doSomething");

        String s = aService.doSomething();
        assertEquals("AService.doSomething", s);
    }

}