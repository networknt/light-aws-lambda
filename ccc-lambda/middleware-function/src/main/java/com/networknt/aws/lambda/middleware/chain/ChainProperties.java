package com.networknt.aws.lambda.middleware.chain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChainProperties {

    boolean audited() default true;
    boolean asynchronous() default false;
    String chainId() default "NULL";
}
