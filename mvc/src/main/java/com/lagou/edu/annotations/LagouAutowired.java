package com.lagou.edu.annotations;

import java.lang.annotation.*;

/**
 * @author kevliu3
 */

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LagouAutowired {
    String value() default "";

}
