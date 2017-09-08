# AWS Lambda, Gdal and Netcdf
Small AWS Lambda function in written in Java. It uses GDAL's Java bindings to extract the minimum and maximum values of geospatial data stored in S3 buckets using GDAL to extract min/max values from geospatial datasets. Proof of concept development.

# Dependencies
The build steps assume the GDAL library and SWIG based Java bindings have been built according to the [documentation here](BuildingGdal.md). Other methods of building GDAL will work, but may require some of the library paths to be updated.

The Lambda function is written in Java, this can be installed with the following command (assuming it was not done as part of the GDAL build process).

    sudo yum install java-1.7.0-openjdk-devel

The deployment bundle is created with Gradle, this can be installed on Amazon Linux with the following steps. Note: these steps use [SDKMAN](http://sdkman.io/) to install Gradle.

    curl -s "https://get.sdkman.io" | bash
    source "/home/ec2-user/.sdkman/bin/sdkman-init.sh"
    sdk install gradle 4.1

You'll also need some data to test against. The lambda function assumes it'll be able to access a GeoTiff/NetCDF file as specified in `sampleinput.json`.

# Build, deployment, and invocation
Clone the repo

    git clone https://github.com/vpac-innovations/AwsLambdaGdalNetcdf.git
    
Copy library files, and gdal.jar into directories that will be packaged into Lambda deployment bundle.

    cp lambda/local/lib/libgdaljni.so AwsLambdaGdalNetcdf/lib/.
    cp lambda/local/lib/libgdal.so.20 AwsLambdaGdalNetcdf/lib/.
    cp lambda/local/lib/libproj.so.12 AwsLambdaGdalNetcdf/lib/.
    cp lambda/local/lib/libnetcdf.so.11 AwsLambdaGdalNetcdf/lib/.
    cp lambda/local/lib/libz.so.1 AwsLambdaGdalNetcdf/lib/.
    cp gdalbuild/gdal-2.2.1/swig/java/gdal.jar AwsLambdaGdalNetcdf/jars/.

Build bundle that will be deployed to Lambda

    cd AwsLambdaGdalNetcdf
    gradle build

Upload build to S3. Due to the size of the included GDAL libraries the only reliable method of uploading a this Lambda function is to first upload the distribution bundle to S3 and then deploy. *Note: the following assumes you've successfully configured the aws cli*

    aws s3api create-bucket --bucket lambda-geospatial-test-code --region ap-southeast-2 \
        --create-bucket-configuration LocationConstraint=ap-southeast-2
    aws s3 cp build/distributions/AwsLambdaGdalNetcdf.zip s3://lambda-geospatial-test-code

Create new Lambda function based on uploaded bundle
    
    aws  lambda create-function --function-name gdalJava --runtime java8 \
        --handler com.vpacinnovations.spatialcube.lambda.GdalHandlers::handleRequest \
        --region ap-southeast-2 --profile default \
        --role <<Insert ARN for lambda role>> \
        --code S3Bucket=lambda-geospatial-test-code,S3Key=AwsLambdaGdalNetcdf.zip
    
**OR**, if you simply want to update the existing function with a new distribution bundle

    aws lambda update-function-code --function-name gdalJava --s3-bucket lambda-geospatial-test-code --s3-key AwsLambdaGdalNetcdf.zip

Finally, you should now be able to invoke the lambda function using the following

    aws lambda invoke --function-name gdalJava --region ap-southeast-2 --payload file://sampleinput.json  outputfile.json

There's a few errors that may occur when you attempt to invoke the function; time outs and out of memory exceptions are common. In this case the timeout and max memory settings can be modified in the Lambda web console; gdalJava (Lambda function) -> Configuration (tab) -> Advanced settings (expandable panel at bottom of config tab).

If all goes according to plan you should see a block of JSON output to `output.json`

    {
        "maximum":669494.4375,
        "minimum":617816.9375,
        "success":true
    }

# Batch invocation

The main method in `GdalInvoker` accepts a bucket name, the class will request a list of all keys within this bucket and launch one lambda request for each key. *Note: careful pointing this at buckets containing a large number of objects*

A batch invocation can be launched via gradle, eg;

    gradle run -Pbucket=lambda-geospatial-test-data

# Notes

## Performance
The test data used in development was a road network proximity NetCDF file, approximately 5GB in size.

Across a 3 runs `gdalinfo` required on average **341 seconds** to extract the minimum and maximum values from the 5GB file.

The single NetCDF file was tiled into 982 smaller NetCDF files and uploaded into an S3 bucket. Using the batch invocation above Lambda+GDAL was able to extract the minimum and maximum values within **18 seconds**.

The Lambda based process includes a number of overheads not encountered by `gdalinfo`, this includes listing the contents of the S3 bucket before the Lambda function calls are made, and each Lambda function also copies the file from S3 to the Lambda functions temp storage before processing. It's expected JVM startup, and loading of the GDAL native libs would add somewhat to execution time. No attempt has been made to optimise tile size, it's expected some improvements could be made here.

All things considered, including this somewhat contrived use case, the performance numbers show there's some good potential here.

## Implementation
The `gdal.registerAll()` call would result in a unsatisfied link error despite setting both `PATH` and `LD_LIBRARY_PATH` lambda environment variables to the location the gdal libs are unzipped too. Hardcoding the full path to each of the required libraries, and loading them via `System.load()` worked around this issue.

Some GDAL drivers support reading of S3 based data directly, this would allow the Lambda function to skip the process of copying the file locally. Unfortunately the NetCDF driver does not offer this support, [more details here](https://trac.osgeo.org/gdal/ticket/6997).




