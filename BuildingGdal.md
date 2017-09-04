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

Set CFLAGS to generate position independent code (support creation of static libs)

    export CFLAGS="-fPIC"

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
    ./config -fPIC --prefix=/home/ec2-user/lambda/local --openssldir=/home/ec2-user/lambda/local/openssl
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
    ./configure --prefix=/home/ec2-user/lambda/local/
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
    
Download and build NetCDF.

    wget https://github.com/Unidata/netcdf-c/archive/v4.4.1.1.zip
    unzip v4.4.1.1.zip
    cd netcdf-c-4.4.1.1
    CPPFLAGS=-I/home/ec2-user/lambda/local/include/ LDFLAGS=-L/home/ec2-user/lambda/local/lib \
        ./configure --enable-netcdf-4 --prefix=/home/ec2-user/lambda/local
    make
    make install
    cd ..

Download and build GDAL.

    wget http://download.osgeo.org/gdal/2.2.1/gdal-2.2.1.tar.gz
    tar -xvf gdal-2.2.1.tar.gz
    cd gdal-2.2.1
    CPPFLAGS=-I/home/ec2-user/lambda/local/include/ \
        LDFLAGS=-L/home/ec2-user/lambda/local/lib \
        PATH=$PATH:/home/ec2-user/lambda/local/bin \
        ./configure --with-curl=/home/ec2-user/lambda/local \
                    --with-hdf5=/home/ec2-user/lambda/local \
                    --prefix=/home/ec2-user/lambda/local \
                    --with-java=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.141.x86_64 \
                    --with-jvm-lib=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.141.x86_64/jre/lib/amd64/server \
                    --with-jvm-lib-add-rpath=yes \
                    --with-java=yes \
                    --with-static-proj4=/home/ec2-user/lambda/local
    make
    make install
    cd ..

Build the GDAL Java SWIG wrappers (libgdaljni.lib, gdal.jar)
    cd gdal-2.2.1/swig/java/
    make
    make install
    cd ../../../

# Verification
Where possible the above build steps generate static libraries that have no dependencies. Lib dependencies can be checked using the `ldd` tool.

    ldd ../lambda/local/lib/libgdal.so

Outputs
<pre>
linux-vdso.so.1 =>  (0x00007ffeeed77000)
<b>libproj.so.12</b> => /home/ec2-user/lambda/local/lib/libproj.so.12 (0x00007fadc3ca7000)
<b>libnetcdf.so.11</b> => /home/ec2-user/lambda/local/lib/libnetcdf.so.11 (0x00007fadc3690000)
libjpeg.so.62 => /usr/lib64/libjpeg.so.62 (0x00007fadc3437000)
libpthread.so.0 => /lib64/libpthread.so.0 (0x00007fadc321b000)
librt.so.1 => /lib64/librt.so.1 (0x00007fadc3012000)
libdl.so.2 => /lib64/libdl.so.2 (0x00007fadc2e0e000)
libz.so.1 => /home/ec2-user/lambda/local/lib/libz.so.1 (0x00007fadc2bf1000)
libstdc++.so.6 => /usr/lib64/libstdc++.so.6 (0x00007fadc28eb000)
libm.so.6 => /lib64/libm.so.6 (0x00007fadc25e9000)
libc.so.6 => /lib64/libc.so.6 (0x00007fadc2225000)
libgcc_s.so.1 => /lib64/libgcc_s.so.1 (0x00007fadc200e000)
/lib64/ld-linux-x86-64.so.2 (0x0000556d61151000)
</pre>

From this we can see the `libgdal.so` library is dependent on `libproj.so.12` and `libnetcdf.so.11`, hence all these files must be included in the bundle deployed to Lambda.


