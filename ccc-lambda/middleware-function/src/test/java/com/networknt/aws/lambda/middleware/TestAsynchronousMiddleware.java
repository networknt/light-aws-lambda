package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

@ChainProperties(asynchronous = true)
public class TestAsynchronousMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(TestAsynchronousMiddleware.class);

    public TestAsynchronousMiddleware(ChainLinkCallback callback, LightLambdaExchange eventWrapper) {
        super(callback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {
        LOG.info("I am executing asynchronously");

        int randomSlept = ThreadLocalRandom.current().nextInt(5, 15);
        LOG.info("I will sleep a total of {} times", randomSlept);

        int slept = 0;
        while (slept < randomSlept) {
            int randomSleep = ThreadLocalRandom.current().nextInt(0, 1000);
            LOG.info("I am sleeping asynchronously for {}ms... ({})", randomSleep, slept);
            Thread.sleep(randomSleep);
            slept++;
        }

        LOG.info("I am done executing asynchronously, doing callback");
        return ChainLinkReturn.successMiddlewareReturn();
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {

    }

}

