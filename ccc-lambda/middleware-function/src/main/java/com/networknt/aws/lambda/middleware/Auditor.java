package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;

import java.util.HashMap;
import java.util.Map;

public class Auditor implements Runnable {

    private final Map<LambdaMiddleware, ChainLinkReturn> auditedMiddleware = new HashMap<>();


    public void add(LambdaMiddleware middleware, ChainLinkReturn middlewareReturn) {
        this.auditedMiddleware.put(middleware, middlewareReturn);
    }

    @Override
    public void run() {
        for (var middleware : this.auditedMiddleware.entrySet()) {
            // TODO
        }
    }
}
