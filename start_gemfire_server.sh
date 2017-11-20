#!/bin/bash

locators="localhost[10334]"

. ./classpath.sh

dir=$( dirname $0 )

gfsh -e "start server --initial-heap=2g --max-heap=2g --name=server --cache-xml-file=$dir/src/main/resources/serverCache.xml --locators=$locators --classpath=$classpath"

