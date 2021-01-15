FROM openjdk:15-jdk as builder
WORKDIR /etc/melijn_backend
COPY ./ ./
USER root
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar

FROM openjdk:15-jdk
WORKDIR /opt/melijn_backend
COPY --from=builder ./etc/melijn_backend/build/libs/ .
ENTRYPOINT java \
    -Xmx250M \
    -jar \
    ./melijn-backend.jar