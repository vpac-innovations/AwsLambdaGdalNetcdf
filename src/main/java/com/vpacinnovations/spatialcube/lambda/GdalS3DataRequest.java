package com.vpacinnovations.spatialcube.lambda;

public class GdalS3DataRequest {
    String s3Bucket;
    String s3Key;

    public GdalS3DataRequest() {

    }

    public GdalS3DataRequest(String bucket, String key) {
        this.s3Bucket = bucket;
        this.s3Key = key;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String S3Bucket) {
        this.s3Bucket = S3Bucket;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String S3Key) {
        this.s3Key = S3Key;
    }
}
