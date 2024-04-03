FROM openjdk:19-slim AS build

WORKDIR /app

RUN apt-get update && apt-get install -y apt-transport-https curl ca-certificates gnupg2 && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add - && \
    apt-get update && apt-get install -y sbt

COPY project/build.properties project/plugins.sbt ./project/
COPY build.sbt ./

COPY src ./src
RUN sbt clean compile

RUN sbt assembly

FROM openjdk:19-slim

RUN groupadd -r akka && useradd -r -g akka akka

WORKDIR /app

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx1024m -XX:+UseG1GC"

HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

COPY --from=build /app/target/scala-3.4.1/*.jar ./routing-service.jar
USER akka
CMD ["sh", "-c", "java $JAVA_OPTS -jar routing-service.jar"]
