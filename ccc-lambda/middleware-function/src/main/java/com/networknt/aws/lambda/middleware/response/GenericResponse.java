package com.networknt.aws.lambda.middleware.response;

import java.lang.reflect.ParameterizedType;

public class GenericResponse<T> {

    @SuppressWarnings({"rawtypes"})
    protected Class reflectClassType() {
        return ((Class) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0]);
    }
}
