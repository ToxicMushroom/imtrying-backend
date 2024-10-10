FROM arm64v8/openjdk:21 as builder
WORKDIR /etc/melijn_backend
COPY ./ ./
USER root
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar

FROM arm64v8/openjdk:21
WORKDIR /opt/melijn_backend
COPY --from=builder ./etc/melijn_backend/build/libs/ .
ENTRYPOINT java \
    -Xmx50M \
    -jar \
    ./melijn-backend.jar