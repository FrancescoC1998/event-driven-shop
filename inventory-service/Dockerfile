# syntax=docker/dockerfile:1
#
# Immagine di inventory-service, costruita in DUE stadi (multi-stage build):
#   1. "build":   immagine con il JDK completo + Maven -> compila e produce il jar
#   2. "runtime": immagine con il solo JRE -> contiene solo il jar finale
# L'immagine finale non contiene sorgenti, Maven né il JDK: è più piccola e più sicura.
#
# Il build context è la ROOT del repository (serve il POM parent e il Maven Wrapper):
#   docker build -f inventory-service/Dockerfile -t shop/inventory-service .
# oppure, più semplicemente: docker compose --profile apps build inventory-service

# ---------------------------------------------------------------------------
# Stadio 1: build
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Prima solo i file di Maven e i POM: Docker mette in cache ogni istruzione,
# quindi se cambia solo il codice Java questi passi non vengono rifatti.
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY inventory-service/pom.xml inventory-service/pom.xml
RUN chmod +x mvnw

# Poi il codice sorgente del modulo
COPY inventory-service/src inventory-service/src

# Compila e impacchetta solo questo modulo (-pl) più ciò da cui dipende (-am).
# --mount=type=cache: la cartella ~/.m2 (dipendenze scaricate) sopravvive tra una build
# e l'altra, così Maven non riscarica tutto ogni volta. Non finisce nell'immagine.
# I test non vengono eseguiti qui: richiedono Docker (Testcontainers) e si lanciano prima, da IDE o mvnw.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -pl inventory-service -am package -Dmaven.test.skip=true

# ---------------------------------------------------------------------------
# Stadio 2: runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl serve all'healthcheck del docker-compose (l'immagine base non lo include).
# Utente senza privilegi: se il processo venisse compromesso, non sarebbe root nel container.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 spring
USER spring

# Dallo stadio "build" prendiamo SOLO il jar eseguibile
COPY --from=build /workspace/inventory-service/target/*.jar app.jar

# Documenta la porta su cui ascolta il servizio (la pubblicazione verso il PC la fa il compose)
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
