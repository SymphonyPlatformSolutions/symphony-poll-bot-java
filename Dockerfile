FROM amazoncorretto:17
WORKDIR /build
COPY ./target/*.jar /app/app.jar
RUN cd /app && jar -xf app.jar && \
    jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    -q \
    --recursive \
    --multi-release 17 \
    --class-path="BOOT-INF/lib/*" \
    --module-path="BOOT-INF/lib/*" \
    app.jar > /deps
RUN jlink \
    --verbose \
    --add-modules $(cat /deps),jdk.naming.dns,jdk.crypto.ec \
    --strip-java-debug-attributes \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /jre

FROM gcr.io/distroless/java-base-debian11
COPY --from=0 /jre /jre
WORKDIR /data/symphony
COPY ./target/*.jar app.jar
ENTRYPOINT [ "/jre/bin/java", "-jar", "./app.jar", "--spring.profiles.active=prod" ]
