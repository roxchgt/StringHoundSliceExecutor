#!/bin/sh
sleep 2
java -Dconfig.file=/prod.conf -Xverify:none -jar /executor.jar
