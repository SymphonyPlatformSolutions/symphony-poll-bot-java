FROM adoptopenjdk/openjdk12:alpine-jre
WORKDIR /data/symphony
COPY ./target/*.jar bot.jar
ENTRYPOINT [ "java", "-Xverify:none", "-jar", "./bot.jar", "--spring.config.location=application.properties" ]
