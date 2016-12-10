FROM java:8
ADD target/colligere-0.1.0-standalone.jar /
ADD config.edn /
ENTRYPOINT ["java","-verbose","-jar","/colligere-0.1.0-standalone.jar"]
