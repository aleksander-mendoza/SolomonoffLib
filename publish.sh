#!/bin/bash
git clone --single-branch --branch repository git@github.com:aleksander-mendoza/SolomonoffLib.git
mvn package -DskipTests -P library
mvn install:install-file  -Dfile=target/solomonoff-$VERSION.jar \
                          -DgroupId=solomonoff \
                          -DartifactId=solomonoff \
                          -Dversion=$VERSION \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=SolomonoffLib/
cd SolomonoffLib
git add .
git commit -m "version $VERSION"
git push
