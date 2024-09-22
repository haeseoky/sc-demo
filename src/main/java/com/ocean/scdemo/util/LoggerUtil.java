package com.ocean.scdemo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtil {
    public static <T> Logger getLogger(Class<T> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
