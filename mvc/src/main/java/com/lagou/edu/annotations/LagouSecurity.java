package com.lagou.edu.annotations;

import java.lang.annotation.*;

/**
 * @author liuku
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LagouSecurity {
    String[] value() default {};

}
