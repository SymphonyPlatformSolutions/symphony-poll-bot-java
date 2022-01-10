FROM ghcr.io/graalvm/jdk:21.3
WORKDIR /data/symphony
COPY ./target/*.jar app.jar
RUN useradd symphony && chmod -R 755 /data/symphony
USER symphony
ENTRYPOINT [ "java", "-jar", "./app.jar", "--spring.profiles.active=prod" ]
