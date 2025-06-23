package com.moola.fx.moneychanger.auth.functions;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * A dummy Lambda Context for unit tests.
 */
public class NoOpLambdaContext implements Context {
    @Override
    public String getAwsRequestId() {
        return "test-request-id";
    }

    @Override
    public String getLogGroupName() {
        return "test-log-group";
    }

    @Override
    public String getLogStreamName() {
        return "test-log-stream";
    }

    @Override
    public String getFunctionName() {
        return "test-function";
    }

    @Override
    public String getFunctionVersion() {
        return "1.0";
    }

    @Override
    public String getInvokedFunctionArn() {
        return "arn:aws:lambda:local:0:function:test-function";
    }

    @Override
    public CognitoIdentity getIdentity() {
        return null;
    }

    @Override
    public ClientContext getClientContext() {
        return null;
    }

    @Override
    public int getRemainingTimeInMillis() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMemoryLimitInMB() {
        return 512;
    }

    @Override
    public LambdaLogger getLogger() {
        // return an anonymous class implementing both log methods
        return new LambdaLogger() {
            @Override
            public void log(String message) {
                System.out.println("Message from Lambda: " + message);
            }
            @Override
            public void log(byte[] message) {
                // no-op
            }
        };
    }
}
