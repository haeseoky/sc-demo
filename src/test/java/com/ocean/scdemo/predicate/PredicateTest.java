package com.ocean.scdemo.predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Objects;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class PredicateTest {
    @Test
    void predicateTest() {
         Predicate<String> predicate = (s) -> s.length() > 0;
         System.out.println(predicate.test("foo")); // true
         System.out.println(predicate.negate().test("foo")); // false
         Predicate<Boolean> nonNull = Objects::nonNull;
         Predicate<Boolean> isNull = Objects::isNull;
         Predicate<String> isEmpty = String::isEmpty;
         Predicate<String> isNotEmpty = isEmpty.negate();
         System.out.println(nonNull.test(true)); // true
         System.out.println(isNull.test(null)); // true
         System.out.println(isEmpty.test("")); // true
         System.out.println(isNotEmpty.test("")); // false
         System.out.println(isNotEmpty.test("abc")); // true

        Predicate<Boolean> booleanPredicate = returnBoolean(true);
        log.info("booleanPredicate: {}", booleanPredicate.test(true));
        assertEquals(false, booleanPredicate.test(true));
    }


    public Predicate<Boolean> returnBoolean(boolean b){
        return (a) -> a == b;
    }

}
