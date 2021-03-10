#!/bin/bash
cd library
git clone --single-branch --branch repository git@github.com:aleksander-mendoza/SolomonoffLib.git
mvn clean package -DskipTests
mvn install:install-file  -Dfile=target/library-$VERSION.jar \
                          -DgroupId=solomonoff \
                          -DartifactId=solomonoff \
                          -Dversion=$VERSION \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=SolomonoffLib/
cd SolomonoffLib
git add .
git commit -m "version $VERSION"
git push
