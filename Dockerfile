FROM adoptopenjdk/openjdk12:alpine-jre
WORKDIR /data/symphony
COPY ./target/*.jar bot.jar
ENTRYPOINT [ "java", "-jar", "./bot.jar" ]
