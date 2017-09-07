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
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.List;

public class GdalInvoker{

    private static class AsyncGdalHandler implements AsyncHandler<InvokeRequest, InvokeResult>
    {
        public void onSuccess(InvokeRequest req, InvokeResult res) {
            System.out.println("\nLambda function returned:");
            ByteBuffer response_payload = res.getPayload();
            System.out.println(new String(response_payload.array()));
        }

        public void onError(Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private static List<GdalS3DataRequest> requestList() {
        List<GdalS3DataRequest> res = new ArrayList<GdalS3DataRequest>();
        res.add(new GdalS3DataRequest(
            "lambda-geospatial-test-data",
            "road-network/RoadNetwork_tile_x9_y21.nc"));
        res.add(new GdalS3DataRequest(
            "lambda-geospatial-test-data",
            "road-network/RoadNetwork_tile_x9_y22.nc"));
        res.add(new GdalS3DataRequest(
            "lambda-geospatial-test-data",
            "road-network/RoadNetwork_tile_x9_y23.nc"));
        return res;
    }

    public static void main(String[] args) throws JsonProcessingException{
        AWSCredentialsProvider cp = new EnvironmentVariableCredentialsProvider();

        AWSLambdaAsync client = 
            AWSLambdaAsyncClientBuilder.standard().withCredentials(cp).build();

        List<GdalS3DataRequest> gdalDataRequests = requestList();

        ObjectMapper mapper = new ObjectMapper();

        List<Future<InvokeResult>> futures = new ArrayList<Future<InvokeResult>>();
        for (GdalS3DataRequest gdr: gdalDataRequests) {
            String inputJson = mapper.writeValueAsString(gdr);
            InvokeRequest req = new InvokeRequest()
                .withFunctionName("gdalJava")
                .withPayload(inputJson);

            AsyncGdalHandler handler = new AsyncGdalHandler();
            Future<InvokeResult> future_res = client.invokeAsync(req, handler);
            futures.add(future_res);
        }

        System.out.print("Waiting for async callback");
        boolean allFuturesDone = false;
        while (!allFuturesDone) {
            int completeCount = 0;
            for (Future<InvokeResult> future_res: futures){
                if (future_res.isDone() || future_res.isCancelled()){
                    completeCount += 1;
                }
            }

            allFuturesDone = completeCount == futures.size();
            System.out.format("%d/%d%n",completeCount,futures.size());
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                System.err.println("Thread.sleep() was interrupted!");
                System.exit(0);
            }
        }

        System.exit(0);

    }

}
