package com.vpacinnovations.spatialcube.lambda;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.vpacinnovations.spatialcube.lambda.S3DataRequest;
import com.vpacinnovations.spatialcube.lambda.S3DataResponse;

public class GdalHandlers implements RequestHandler<S3DataRequest, S3DataResponse> {

    private LambdaLogger logger;

    /**
     * Function downloads an object from s3 and saves to the local file system of 
     * the Lambda function. Objects are saved to the tmp directory, using the key
     * as the filename.
     * @param bucketName name of the bucket containing object to download
     * @param key s3 key used to store the object.
     * @return The full path to the local file as a string
     */ 
    protected String downloadFile(String bucketName, String key) throws IOException {
        String localFilename = "/tmp/" + bucketName + "/" + key;
        Path path = Paths.get(localFilename);

        //check if file has already been copied to this lambda instance.
        if (Files.exists(path)) {
            return localFilename;
        }

        Path parentPath = path.getParent();
        Files.createDirectories(parentPath);

        //Lambda env variables are set base on the role that executes the Lambda function.
        //This role must have permission to access the s3 bucket.
        AmazonS3 s3Client = new AmazonS3Client(new EnvironmentVariableCredentialsProvider());
        try {
            S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, key));
            logger.log("Content-Type: " + s3object.getObjectMetadata().getContentType());
            
            GetObjectRequest objectRequest = new GetObjectRequest(bucketName, key);
            S3Object objectPortion = s3Client.getObject(objectRequest);
            InputStream objectData = objectPortion.getObjectContent();
            Files.copy(objectData, path);
            objectPortion.close();
            
        } catch (AmazonServiceException ase) {
            logger.log("Caught an AmazonServiceException, which" +
            		" means your request made it " +
                        "to Amazon S3, but was rejected with an error response" +
                        " for some reason.");
            logger.log("Error Message:    " + ase.getMessage());
            logger.log("HTTP Status Code: " + ase.getStatusCode());
            logger.log("AWS Error Code:   " + ase.getErrorCode());
            logger.log("Error Type:       " + ase.getErrorType());
            logger.log("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            logger.log("Caught an AmazonClientException, which means"+
            		" the client encountered " +
                        "an internal error while trying to " +
                        "communicate with S3, " +
                        "such as not being able to access the network.");
            logger.log("Error Message: " + ace.getMessage());
        }
        return localFilename;
    }

    public S3DataResponse handleRequest(S3DataRequest request, Context context) {
        logger = context.getLogger();
        logger.log("received S3Bucket: " + request.getS3Bucket());
        logger.log("received S3Key: " + request.getS3Key());

        //These libraries should be found by Java after setting the LD_LIBRARY_PATH
        //unfortunately this seems to not work in the Lambda environment. Hence the
        //native libraries requried by gdal.jar are loaded as follows.
        //Requires more investigation...
        System.load("/var/task/lib/libhdf5.so.10");
        System.load("/var/task/lib/libhdf5_hl.so.10");
        System.load("/var/task/lib/libnetcdf.so.11");
        System.load("/var/task/lib/libproj.so.12");
        System.load("/var/task/lib/libgdal.so");
        System.load("/var/task/lib/libgdaljni.so");
        gdal.AllRegister();

        String gdalver = gdal.VersionInfo();
        logger.log("GDAL version " + gdalver);

        String s3bucketName = request.getS3Bucket();
        String s3key = request.getS3Key();

        double minmax[] = null;
        try {
            //Some, but not all, GDAL drivers support reading data directly from s3. To
            //work around this we download the file locally for processing with GDAL.
            String localFilename = downloadFile(s3bucketName, s3key);

            logger.log("GDAL Open: " + localFilename);
            Dataset ds = gdal.Open(localFilename);
            if (ds == null) {
                logger.log("GDAL could not open Dataset");
            } else {
                minmax = new double[2];
                Band b = ds.GetRasterBand(1); // bands are indexed from 1
                b.ComputeRasterMinMax(minmax);

                logger.log(String.format("min: %s,   max: %s", minmax[0], minmax[1]));

                Driver dsDriver = ds.GetDriver();
                logger.log("Driver: " + dsDriver.getShortName() + "/" + dsDriver.getLongName());
            }

        } catch (IOException e) {
            logger.log("failed to save s3 download");
            e.printStackTrace();
        }

        //minmax will remain null if the min/max fail to compute. The S3DataResponse
        //class constructor sets a success flag based on whether a null value is passed in. 
        return new S3DataResponse(minmax);
    }
}

