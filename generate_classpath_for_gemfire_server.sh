#!/bin/bash

# Extract any JAR files required for Gemfire server processes, from the Spring Boot
# uber-JAR

mkdir -p ./lib

echo "Copying all project JARs into ./lib"
./mvnw dependency:copy-dependencies

cp=$PWD/target/classes
for jar in $( find $PWD/lib -name '*.jar' | egrep -v '^/.+?/(gemfire|geode)\-.+?\.jar$' )
do
  cp+=":$jar"
done
echo "Add the following to the CLASSPATH for the Gemfire server startup:"
echo
echo $cp

