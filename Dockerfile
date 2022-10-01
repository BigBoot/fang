FROM gradle:7.5.1-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon 

FROM openjdk:11-jre-slim

EXPOSE 8080

RUN mkdir /app
RUN mkdir /data

COPY --from=build /home/gradle/src/build/libs/*-all.jar /app/app.jar

ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-Djava.security.egd=file:/dev/./urandom", "-Duser.dir=/data/", "-jar", "/app/app.jar"]