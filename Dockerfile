FROM azul/zulu-openjdk-alpine:21-jre

RUN addgroup -S wiki && adduser -S wiki -G wiki

COPY --chown=wiki:wiki build/libs/wiki-service.jar /wikiservice.jar

USER wiki
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/wikiservice.jar"]
