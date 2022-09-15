FROM openjdk:11
ADD files/* /
CMD ["java", "-Dconfig.file=/prod.conf", "-Xverify:none", "-jar", "/executor.jar"]
