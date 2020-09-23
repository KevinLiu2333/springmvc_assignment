package com.lagou.edu.annotations;

import java.lang.annotation.*;

/**
 * @author kevliu3
 */

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LagouService {
    String value() default "";

}
