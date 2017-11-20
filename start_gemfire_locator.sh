#!/bin/bash

ext_hostname="ec2-34-213-83-19.us-west-2.compute.amazonaws.com"

. ./classpath.sh

gfsh -e "start locator --name=locator --hostname-for-clients=$ext_hostname --classpath=$classpath"

