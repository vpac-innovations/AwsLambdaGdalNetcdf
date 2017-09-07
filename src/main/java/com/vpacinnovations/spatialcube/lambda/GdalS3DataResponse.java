package com.vpacinnovations.spatialcube.lambda;

public class GdalS3DataResponse {
    double maximum;
    double minimum;
    boolean success;

    public GdalS3DataResponse() {
        this.maximum = 0;
        this.minimum = 0;
        this.success = false;
    }

    public GdalS3DataResponse(double[] minmax) {
        if (minmax == null) {
            success = false;
        } else {
            success = true;
            minimum = minmax[0];
            maximum = minmax[1];
        }
    }

    public double getMinimum() {
        return minimum;
    }

    public void setMinimum(double minimum) {
        this.minimum = minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public void setMaximum(double maximum) {
        this.maximum = maximum;
    }

    public boolean getSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
