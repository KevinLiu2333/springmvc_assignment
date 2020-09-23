package com.lagou.edu.annotations;

import java.lang.annotation.*;

/**
 * @author kevliu3
 */

@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LagouRequestMapping {
    String value() default "";

}
