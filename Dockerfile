FROM openjdk:17-oracle
ARG JAR_FILE=target/TextSearcherV2-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]