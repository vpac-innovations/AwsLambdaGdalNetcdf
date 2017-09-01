package com.vpacinnovations.spatialcube.lambda;

public class S3DataRequest {
    String s3Bucket;
    String s3Key;

    public S3DataRequest() {

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
