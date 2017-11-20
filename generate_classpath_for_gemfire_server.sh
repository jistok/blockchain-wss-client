#!/bin/bash

mkdir -p ./lib

echo "Copying all project JARs into ./lib"
./mvnw dependency:copy-dependencies

cp=$PWD/target/classes
for jar in $( find $PWD/lib -name '*.jar' | egrep -v '^/.+?/(gemfire|geode|json|android-json)\-.+?\.jar$' )
do
  cp+=":$jar"
done
echo "Writing ./classpath.sh"
echo "classpath=\"$cp\"" > ./classpath.sh

