# event-driven-shop

Progetto didattico a microservizi: Spring Boot 4.1 / Java 25, Kafka (KRaft), PostgreSQL, Docker Compose.
Vedi README.md per architettura, spiegazioni e roadmap.
Stato: tappa 1 completata (infrastruttura + schema DB via Flyway); prossima: tappa 2 (`inventory-service`).

## Comandi
- Infrastruttura: `docker compose up -d` (Postgres 5432, Kafka 9092, Kafka UI http://localhost:8085)
- Migrazioni: `docker compose run --rm flyway-inventory` / `docker compose run --rm flyway-orders` (aggiungi `info` per lo stato)
- Maven: sempre il wrapper `mvnw` / `mvnw.cmd`, con JAVA_HOME = JDK 25 (`C:\Program Files\Java\jdk-25.0.5.0.2`) impostato solo nella sessione.
  Il JAVA_HOME di sistema è Java 8 e NON va modificato.
- Password e porte in `.env` (non committato); modello in `.env.example`.

## Regole di architettura
- Un database per servizio (`orders_db` / `orders_user`, `inventory_db` / `inventory_user`): mai query, JOIN o foreign key tra database diversi.
- Schema solo via Flyway in `<servizio>/src/main/resources/db/migration`; mai modificare una migrazione già applicata, aggiungerne una nuova. Hibernate con `ddl-auto: validate`.
- Topic Kafka creati esplicitamente (auto-create disattivato sul broker).
- Comunicazione tra servizi: REST per richieste sincrone, eventi Kafka per tutto ciò che "è successo" (saga a coreografia).
- Prezzi e importi: `NUMERIC` in SQL, `BigDecimal` in Java, mai float/double.
- Da un container usare il nome del servizio (`postgres:5432`, `kafka:29092`); dal PC `localhost:5432`, `localhost:9092`.

## Ambiente
- Windows; da Git Bash anteporre `MSYS_NO_PATHCONV=1` ai comandi `docker exec ... /opt/...`.
- Gli script `.sh` devono avere fine riga LF (garantito da `.gitattributes`).
