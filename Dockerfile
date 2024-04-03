FROM openjdk:19-slim AS build

WORKDIR /app

RUN apt-get updateHealth && apt-get install -y apt-transport-https curl ca-certificates gnupg2 && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add - && \
    apt-get updateHealth && apt-get install -y sbt

COPY project/build.properties project/plugins.sbt ./project/
COPY build.sbt ./

RUN sbt updateHealth

COPY src ./src
RUN sbt clean compile

RUN sbt assembly

FROM openjdk:19-slim

WORKDIR /app

EXPOSE 8080

COPY --from=build /app/target/scala-3.4.1/*.jar ./routing-service.jar

CMD ["java", "-jar", "routing-service.jar"]
