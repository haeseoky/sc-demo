package com.ocean.scdemo.interceptor;

import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomMethodHandler implements MethodInterceptor {

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        log.info("Before method " + method.getName());
        Object result = proxy.invokeSuper(obj, args);
        log.info("After method " + method.getName());
        return result;
    }
}
