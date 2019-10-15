FROM adoptopenjdk/openjdk12:alpine-jre
WORKDIR /data/symphony
COPY ./target/*.jar bot.jar
ENTRYPOINT [ "java", "-jar", "./bot.jar", "--spring.config.location=application.properties", "-noverify", "-XX:TieredStopAtLevel=1", "-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI", "-XX:+UseJVMCICompiler" ]
