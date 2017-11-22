#!/bin/bash

jar="blockchain-wss-client-0.0.1-SNAPSHOT.jar"
log_file="blockchain-wss-client.log"

nohup java -Xmx1g -Xms1g -jar $jar </dev/null >>$log_file 2>&1 &

