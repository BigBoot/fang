FROM gradle:6.7.1-jdk8 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon 

FROM openjdk:8-jre-slim

EXPOSE 8080

RUN mkdir /app
RUN mkdir /data

COPY --from=build /home/gradle/src/build/libs/*-all.jar /app/app.jar

WORKDIR /data/
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]