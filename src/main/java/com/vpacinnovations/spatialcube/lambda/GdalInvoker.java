package com.vpacinnovations.spatialcube.lambda;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;

public class GdalInvoker{

    public static void main(String[] args) {
        AWSCredentialsProvider cp = new EnvironmentVariableCredentialsProvider();

        final GdalService gdalService = LambdaInvokerFactory.builder()
            .lambdaClient(AWSLambdaClientBuilder.standard().withCredentials(cp).build())
            .build(GdalService.class);

        S3DataRequest input = new S3DataRequest(
            "lambda-geospatial-test-data",
            "road-network/RoadNetwork_tile_x9_y23.nc");

        S3DataResponse output = gdalService.getGdalData(input);
        if (output.getSuccess()) {
            System.out.println(
                String.format("min %s, max %s", 
                              output.getMinimum(), 
                              output.getMaximum()));
        } else {
            System.out.println("failed to get min/max");
        }

    }

}
