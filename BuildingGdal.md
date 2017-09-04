# Building GDAL libraries for AWS Lambda based deployment
The following doc was pieced together from a number of other walkthoughs. 
- [Running Python with compiled code on AWS Lambda](http://www.perrygeo.com/running-python-with-compiled-code-on-aws-lambda.html)
- [joshtkehoe/lambda-python-gdal](https://github.com/joshtkehoe/lambda-python-gdal)
- [hectcastro/lambda-gdalinfo](https://github.com/hectcastro/lambda-gdalinfo)

Dependencies have been updated, and additional steps have been added to support NetCDF and building of GDAL's Java bindings.

# Build environment
Lambda functions are executed within an Amazon Linux environment. It is recommended to build the native libaries (GDAL) packaged with your Lambda function in this environment to ensure they do not rely on shared libraries that may not be available.  The following build steps were performed on an instance launched from the Amazon Linux AMI (Amazon Linux AMI 2017.03.1 (HVM), SSD Volume Type).

# Build steps
Install build dependencies

    sudo yum install python27-devel python27-pip gcc libjpeg-devel \
        zlib-devel gcc-c++ automake libtool java-1.7.0-openjdk-devel \
        swig ant

Make directory to store GDAL Build and dependencies

    mkdir gdalbuild; cd gdalbuild

Download and build proj.4

    wget https://github.com/OSGeo/proj.4/archive/4.9.3.tar.gz
    tar -zvxf 4.9.3.tar.gz
    cd proj.4-4.9.3/
    ./autogen.sh
    ./configure --prefix=/home/ec2-user/lambda/local
    make
    make install
    cd ..

Download and build OpenSSL. This is used to provide SSL/HTTPS support in the curl lib.

    wget https://www.openssl.org/source/openssl-1.0.2l.tar.gz
    tar xvf openssl-1.0.2l.tar.gz
    cd openssl-1.0.2l
    ./config  --prefix=/home/ec2-user/lambda/local --openssldir=/home/ec2-user/lambda/local/openssl
    make
    make install
    cd ..

Download and build curl. GDAL support of direct access to AWS S3 buckets requires the curl library. *Note: at the completion of the configure set please ensure HTTPS is included in the list of supported protocols.*

    wget https://curl.haxx.se/download/curl-7.55.0.tar.gz
    tar xvf curl-7.55.0.tar.gz
    cd curl-7.55.0
    ./configure --prefix=/home/ec2-user/lambda/local --with-ssl=/home/ec2-user/lambda/local --disable-shared
    make
    make install
    cd ..

Download and build zlib. zlib is a compression library used by HDF5.

    wget https://zlib.net/zlib-1.2.11.tar.gz
    tar xvf zlib-1.2.11.tar.gz
    cd zlib-1.2.11
    ./configure --prefix=/home/ec2-user/lambda/local/ --static
    make
    make install
    cd ..

Download and build HDF5 lib. HDF5 is the file format used by NetCDF and is a dependency of the NetCDF library.

    wget https://support.hdfgroup.org/ftp/HDF5/current18/src/hdf5-1.8.19.tar
    tar xvf hdf5-1.8.19.tar
    cd hdf5-1.8.19
    ./configure --with-zlib=/home/ec2-user/lambda/local/ --prefix=/home/ec2-user/lambda/local/ --disable-shared
    make
    make install
    cd ..
    
    
    


