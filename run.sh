#!/bin/sh

#first run:  "gradle compileToBin" to compile and put everything in ./bin

java -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog \
     -Dorg.apache.commons.logging.simplelog.defaultlog=DEBUG \
     -Dorg.apache.commons.logging.simplelog.log.org.apache.http=INFO \
     -Dorg.apache.commons.logging.simplelog.log.org.apache.http.wire=INFO \
     -jar `ls -d1 ./bin/cleanas2-server-*.jar` \
     home/config.json
