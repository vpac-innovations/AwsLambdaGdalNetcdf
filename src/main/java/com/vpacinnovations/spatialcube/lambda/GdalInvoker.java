package com.vpacinnovations.spatialcube.lambda;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

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

    /**
     * Method used the S3 API to request a list of all objects within a bucket.
     * From this it builds a list of request objects.
     * @param bucketName name of the S3 bucket
     * @return a list of all objects (as GdalS3DataRequest's) within a S3 bucket. 
     **/
    private static List<GdalS3DataRequest> requestList(String bucketName) 
        throws AmazonServiceException, AmazonClientException{

        List<GdalS3DataRequest> res = new ArrayList<GdalS3DataRequest>();

        AmazonS3 s3Client = new AmazonS3Client(new EnvironmentVariableCredentialsProvider());
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result;

        do {
            result = s3Client.listObjectsV2(req);
            result.setMaxKeys(20);
            for (S3ObjectSummary objectSummary: result.getObjectSummaries()) {
                GdalS3DataRequest dr = new GdalS3DataRequest(bucketName, 
                    objectSummary.getKey());
                res.add(dr);
            }
        } while (result.isTruncated() == true);

        //return res.subList(0,10); //for testing and cost reduction during dev
        return res;
    }

    public static void main(String[] args) throws JsonProcessingException, 
            AmazonServiceException, AmazonClientException {
        
        System.out.println("Processing bucket " + args[0]);

        AWSCredentialsProvider cp = new EnvironmentVariableCredentialsProvider();
        AWSLambdaAsync client = 
            AWSLambdaAsyncClientBuilder.standard().withCredentials(cp).build();

        List<GdalS3DataRequest> gdalDataRequests;
        gdalDataRequests = requestList(args[0]);

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

        System.out.print("Waiting for async callbacks...");
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
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                System.err.println("Thread.sleep() was interrupted!");
                System.exit(0);
            }
        }

        System.exit(0);

    }

}
