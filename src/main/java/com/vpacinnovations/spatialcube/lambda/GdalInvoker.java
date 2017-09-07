package com.vpacinnovations.spatialcube.lambda;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

public class GdalInvoker{

    private static class AsyncGdalHandler implements AsyncHandler<InvokeRequest, InvokeResult>
    {
        public void onSuccess(InvokeRequest req, InvokeResult res) {
            System.out.println("\nLambda function returned:");
            ByteBuffer response_payload = res.getPayload();
            System.out.println(new String(response_payload.array()));
            System.exit(0);
        }

        public void onError(Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) throws JsonProcessingException{
        AWSCredentialsProvider cp = new EnvironmentVariableCredentialsProvider();

        AWSLambdaAsync client = 
            AWSLambdaAsyncClientBuilder.standard().withCredentials(cp).build();

        GdalS3DataRequest input = new GdalS3DataRequest(
            "lambda-geospatial-test-data",
            "road-network/RoadNetwork_tile_x9_y23.nc");

        ObjectMapper mapper = new ObjectMapper();
        String inputJson = mapper.writeValueAsString(input);

        InvokeRequest req = new InvokeRequest()
            .withFunctionName("gdalJava")
            .withPayload(inputJson);

        AsyncGdalHandler handler = new AsyncGdalHandler();
        Future<InvokeResult> future_res = client.invokeAsync(req, handler);

        System.out.print("Waiting for async callback");
        while (!future_res.isDone() && !future_res.isCancelled()) {
            // perform some other tasks...
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                System.err.println("Thread.sleep() was interrupted!");
                System.exit(0);
            }
            System.out.print(".");
        }

    }

}
