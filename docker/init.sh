!#/bin/sh
sleep 3
java -Dconfig.file=/prod.properties -Xverify:none -jar /worker.jar
