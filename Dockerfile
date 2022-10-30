FROM adoptopenjdk/openjdk11:jre-11.0.10_9-centos
ARG JAR_FILE=target/casio-broker-*.jar
COPY ${JAR_FILE} app.jar
ENV TZ=Asia/Hong_Kong
EXPOSE 8080
ENTRYPOINT ["java","-jar", "-Xmx6144M", "/app.jar"]
