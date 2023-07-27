package com.networknt.aws.lambda.middleware.chain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChainProperties {
    String DEFAULT_CHAIN_ID = "NULL";
    String DEFAULT_LOG_KEY = "NULL";
    boolean audited() default true;
    boolean asynchronous() default false;
    String id() default DEFAULT_CHAIN_ID;
    String logKey() default  DEFAULT_LOG_KEY;
}
