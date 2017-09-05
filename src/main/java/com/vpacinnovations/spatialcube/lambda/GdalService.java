package com.vpacinnovations.spatialcube.lambda;

import com.amazonaws.services.lambda.invoke.LambdaFunction;

public interface GdalService {
    @LambdaFunction(functionName="gdalJava")
    S3DataResponse getGdalData(S3DataRequest input);
}

