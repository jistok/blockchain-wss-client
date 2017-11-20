#!/bin/bash

for i in `jps | egrep '^[0-9]+ +.+?Launcher$' | awk '{print $1}'` ; do kill $i ; done

