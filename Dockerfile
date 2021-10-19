FROM openjdk:15-jdk
RUN jlink --no-header-files --no-man-pages --compress=2 --strip-debug --output /jvm --add-modules \
java.base,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.naming,java.xml,jdk.net,\
java.rmi,java.security.jgss,java.security.sasl,java.sql,jdk.naming.dns,jdk.httpserver,jdk.unsupported,jdk.crypto.ec
WORKDIR /build
COPY ./target/*.jar bot.jar
RUN jar -xf bot.jar
RUN mkdir /app && cp -r META-INF /app && cp -r BOOT-INF/classes/* /app

FROM debian:stretch-slim
WORKDIR /data/symphony
COPY --from=0 /jvm /jvm
COPY --from=0 /build/BOOT-INF/lib /lib
COPY --from=0 /app .
ENTRYPOINT [ "/jvm/bin/java", "-cp", ".:/lib/*", "com.symphony.ps.pollbot.PollBot", "--spring.profiles.active=prod" ]
