#!/bin/bash

# Start streaming web server
java -cp "$CLASS_PATH:./src/main/resources:./target/*:./target/dependency-jars/*" awsPrototype.ApplicationStart $@ 1>>/tmp/rapidoid-http-server.log 2>>/tmp/rapidoid-http-server.err &
ps -Af|grep java

