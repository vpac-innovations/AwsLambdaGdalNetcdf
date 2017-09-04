# Building GDAL libraries for AWS Lambda based deployment
The following doc was pieced together from a number of other walkthoughs. 
- [Running Python with compiled code on AWS Lambda](http://www.perrygeo.com/running-python-with-compiled-code-on-aws-lambda.html)
- [joshtkehoe/lambda-python-gdal](https://github.com/joshtkehoe/lambda-python-gdal)
- [hectcastro/lambda-gdalinfo](https://github.com/hectcastro/lambda-gdalinfo)

Dependencies have been updated, and additional steps have been added to support NetCDF and building of GDAL's Java bindings.

# Build environment
Lambda functions are executed within an Amazon Linux environment, it is therefore necessary to perform the following build on an instance launched from an Amazon Linux AMI (following was performed on Amazon Linux AMI 2017.03.1 (HVM), SSD Volume Type).




