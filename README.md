# event-driven-shop

Progetto didattico: una **mini gestione ordini a microservizi** con Spring Boot, Kafka, PostgreSQL e Docker.
Più avanti si aggiungeranno un frontend React (Vite + TypeScript) e un server MCP per interrogare il sistema con un LLM.

> Stato attuale: **tappa 2 completata**: infrastruttura, schema dei database e primo microservizio (`inventory-service`, CRUD dei prodotti), eseguibile da IntelliJ o in Docker.

---

## Indice

1. [Architettura obiettivo](#1-architettura-obiettivo)
2. [Stack e versioni](#2-stack-e-versioni)
3. [Avvio rapido](#3-avvio-rapido)
4. [Struttura del progetto](#4-struttura-del-progetto)
5. [Cosa abbiamo fatto finora](#5-cosa-abbiamo-fatto-finora)
   - [5.1 Progetto Maven multi-modulo](#51-progetto-maven-multi-modulo)
   - [5.2 Docker Compose](#52-docker-compose)
   - [5.3 PostgreSQL: un database per servizio](#53-postgresql-un-database-per-servizio)
   - [5.4 Flyway: lo schema dei database](#54-flyway-lo-schema-dei-database)
   - [5.5 Kafka](#55-kafka)
   - [5.6 Rete: localhost o nome del servizio?](#56-rete-localhost-o-nome-del-servizio)
   - [5.7 inventory-service](#57-inventory-service)
   - [5.8 Il servizio in Docker](#58-il-servizio-in-docker)
6. [Strumenti: DBeaver e Kafka UI](#6-strumenti-dbeaver-e-kafka-ui)
7. [Comandi utili](#7-comandi-utili)
8. [Problemi noti e soluzioni](#8-problemi-noti-e-soluzioni)
9. [Roadmap](#9-roadmap)

---

## 1. Architettura obiettivo

```
  React (Vite)          [poi] mcp-server
       │                      │
       └──────────┬───────────┘
                  ▼
           api-gateway  (Spring Cloud Gateway)
           │                  │
     order-service      inventory-service      notification-service
     (orders_db)        (inventory_db)         (solo consumer Kafka)
           └──────── Kafka ────────┘
```

| Servizio | Responsabilità | Database |
|---|---|---|
| `inventory-service` | Catalogo prodotti e stock | `inventory_db` |
| `order-service` | Ciclo di vita degli ordini (`PENDING` → `CONFIRMED` / `REJECTED`) | `orders_db` |
| `notification-service` | Avvisa il cliente dell'esito dell'ordine | nessuno |
| `api-gateway` | Unico punto d'ingresso per frontend e MCP | nessuno |
| `common-events` | Libreria con i DTO degli eventi Kafka (non è un servizio) | — |

Flusso di un ordine (**saga a coreografia**): ogni servizio reagisce agli eventi degli altri, senza un coordinatore centrale.

```
order-service      → salva ordine PENDING        → pubblica OrderCreated
inventory-service  ← OrderCreated → scala stock  → pubblica StockReserved / StockRejected
order-service      ← StockReserved / Rejected    → ordine CONFIRMED / REJECTED → pubblica OrderConfirmed / OrderRejected
notification-svc   ← OrderConfirmed / Rejected   → avvisa il cliente
```

---

## 2. Stack e versioni

| Componente | Versione | Note |
|---|---|---|
| Java | **25** (Oracle JDK) | Solo per questo progetto: `JAVA_HOME` di sistema resta su Java 8 |
| Spring Boot | 4.1.2 | Parent del progetto Maven |
| Spring Cloud | 2025.1.4 | BOM importato, servirà per l'api-gateway |
| Maven | 3.9.16 | Tramite Maven Wrapper (`mvnw`) |
| PostgreSQL | 17 | Immagine `postgres:17` |
| Apache Kafka | 4.3.2 | Immagine `apache/kafka`, modalità KRaft |
| Kafka UI | 1.5.0 | Immagine `ghcr.io/kafbat/kafka-ui` |
| Flyway | 13.11.1 | Immagine `flyway/flyway` (CLI) |

---

## 3. Avvio rapido

**Prerequisiti:** Docker Desktop avviato, JDK 25 installato.

```bash
# 1. Crea il file con password e porte (poi cambia i valori "change-me")
cp .env.example .env

# 2. Avvia l'infrastruttura (Postgres, Kafka, Kafka UI)
docker compose up -d

# 3. Crea le tabelle
docker compose run --rm flyway-inventory
docker compose run --rm flyway-orders

# 4. Avvia i microservizi in Docker (oppure lanciali da IntelliJ)
docker compose --profile apps up -d --build
```

| Cosa | Dove |
|---|---|
| inventory-service | http://localhost:8081/products |
| Health del servizio | http://localhost:8081/actuator/health |
| Kafka UI | http://localhost:8085 |
| PostgreSQL | `localhost:5432` |
| Kafka (dalle app sul PC) | `localhost:9092` |

Per usare Maven con Java 25 in un terminale PowerShell (solo per quella sessione):

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-25.0.5.0.2"; $env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd validate
```

---

## 4. Struttura del progetto

```
event-driven-shop/
├── pom.xml                         ← POM parent (aggregatore dei moduli)
├── mvnw, mvnw.cmd, .mvn/           ← Maven Wrapper
├── docker-compose.yml              ← infrastruttura
├── .env.example                    ← modello delle variabili (il vero .env NON è su git)
├── .gitignore, .gitattributes
├── docker/
│   └── postgres/init/
│       └── 01-create-databases.sh  ← crea utenti e database al primo avvio
├── .dockerignore                   ← file esclusi dalle build delle immagini
├── postman/                        ← collection Postman con le richieste di prova
├── inventory-service/              ← modulo Maven (tappa 2)
│   ├── pom.xml
│   ├── Dockerfile
│   ├── products.http               ← richieste di prova per IntelliJ
│   └── src/
│       ├── main/java/it/francesco/shop/inventory/
│       │   ├── InventoryServiceApplication.java
│       │   ├── product/            ← controller, service, repository, entità, dto/
│       │   └── common/             ← GlobalExceptionHandler
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/       ← V1__create_products.sql, V2__seed_products.sql
│       └── test/java/...           ← Testcontainers + test delle API
└── order-service/
    └── src/main/resources/db/migration/
        └── V1__create_orders.sql
```

`order-service` per ora contiene **solo le migrazioni SQL**: diventerà un modulo Maven nella tappa 3.

---

## 5. Cosa abbiamo fatto finora

### 5.1 Progetto Maven multi-modulo

Il [`pom.xml`](pom.xml) radice è un **POM parent** (`<packaging>pom</packaging>`): non produce un jar, serve a:

- **aggregare** i moduli (`<modules>`, per ora vuoto): `mvnw package` dalla radice compilerà tutto;
- **centralizzare** la configurazione: Java 25 e le versioni delle librerie valgono per tutti i moduli.

Concetti chiave:

| Elemento | Significato |
|---|---|
| `<parent>` | Da chi **eredito** (Spring Boot): versioni delle librerie e plugin già configurati |
| `<modules>` | Chi **aggrego**: i sottoprogetti da compilare insieme |
| `<dependencyManagement>` | Fissa solo le **versioni**, non aggiunge dipendenze |
| BOM (`<scope>import</scope>`) | Elenco di versioni testate insieme; quello di Spring Cloud va importato perché un progetto può avere un solo `<parent>` |

Catena di ereditarietà (quando aggiungeremo i moduli):

```
spring-boot-starter-parent  ←  shop-parent  ←  inventory-service, order-service, ...
```

Il **Maven Wrapper** (`mvnw`) scarica la versione di Maven fissata nel progetto, così il build è identico su ogni PC.

### 5.2 Docker Compose

[`docker-compose.yml`](docker-compose.yml) definisce l'infrastruttura. Concetti:

- **Immagine vs container**: l'immagine è il modello (come una classe Java), il container è un'istanza in esecuzione (come un oggetto).
- **Un processo per container**: Postgres, Kafka e Kafka UI sono container separati, aggiornabili e riavviabili in modo indipendente. Docker Desktop li mostra raggruppati sotto il progetto `shop` (dalla riga `name: shop`).
- **Variabili `${...}`**: vengono lette dal file `.env`. Così porte e password si cambiano senza toccare il compose condiviso su git.
- **Volumi con nome** (`pgdata`, `kafkadata`): i dati sopravvivono a `docker compose down`. Si cancellano solo con `down -v`.
- **Healthcheck + `depends_on: condition: service_healthy`**: un container parte solo quando quello da cui dipende è davvero pronto.
- **Profili** (`profiles: ["migrate"]`): i servizi Flyway non partono con `up`, si lanciano a mano con `docker compose run`.

| Container | Immagine | Ruolo |
|---|---|---|
| `shop-postgres` | `postgres:17` | Database |
| `shop-kafka` | `apache/kafka:4.3.2` | Broker Kafka (dove vivono topic e messaggi) |
| `shop-kafka-ui` | `kafbat/kafka-ui` | Interfaccia web per osservare Kafka (sta a Kafka come DBeaver sta a Postgres) |
| *(usa e getta)* | `flyway/flyway` | Applica le migrazioni e si cancella (`run --rm`) |

### 5.3 PostgreSQL: un database per servizio

Una sola istanza Postgres, ma **un database e un utente per ogni servizio**:

| Database | Utente | Usato da |
|---|---|---|
| `orders_db` | `orders_user` | `order-service` |
| `inventory_db` | `inventory_user` | `inventory-service` |

Lo script [`01-create-databases.sh`](docker/postgres/init/01-create-databases.sh) li crea e toglie a tutti gli altri il permesso di connessione (`REVOKE CONNECT ... FROM PUBLIC`).
Risultato: `orders_user` **non può** aprire `inventory_db` (`User does not have CONNECT privilege`). Così il principio "ogni servizio possiede i suoi dati" è garantito dai permessi, non solo dalla buona volontà: niente JOIN tra ordini e magazzino, i servizi dovranno parlarsi via API o eventi.

> ⚠️ Gli script in `docker-entrypoint-initdb.d` girano **solo al primo avvio**, con il volume vuoto. Se si modifica lo script serve `docker compose down -v` (che cancella i dati).

I database `postgres`, `template0` e `template1` sono di sistema e si possono ignorare.

### 5.4 Flyway: lo schema dei database

Le tabelle **non** si creano a mano (es. da DBeaver) né con `ddl-auto` di Hibernate: si creano con **migrazioni Flyway**, file SQL numerati e versionati con il codice.

| | Script di init (`01-create-databases.sh`) | Migrazioni Flyway (`V1__...sql`) |
|---|---|---|
| Crea | Utenti e database | Tabelle e dati |
| Eseguito da | Postgres, al primo avvio | Flyway |
| Quando | Solo con volume vuoto | Ogni volta che ci sono migrazioni nuove |

Come funziona: Flyway esegue i file in ordine di versione e registra ciascuno (con una *checksum*) nella tabella `flyway_schema_history`. Alla volta successiva esegue solo quelli nuovi.

**Regola d'oro:** un file già eseguito non si modifica mai. Per cambiare lo schema si aggiunge `V3__...sql`.

Per ora Flyway gira come **CLI in Docker** (servizi `flyway-inventory` e `flyway-orders`). I file stanno già in `src/main/resources/db/migration`, la cartella standard di Spring Boot: quando creeremo i servizi Java, il loro Flyway leggerà gli stessi file, vedrà in `flyway_schema_history` che sono già applicati e non farà nulla.

Schema creato:

**`inventory_db`**
- `products`: `id` (UUID), `sku` (univoco), `name`, `description`, `price`, `stock`, `version`, `created_at`, `updated_at`
- 8 prodotti di esempio con UUID fissi; `LP-001` ha **stock 0** apposta, per provare un ordine rifiutato.

**`orders_db`**
- `orders`: `id`, cliente, `status` (`PENDING` / `CONFIRMED` / `REJECTED`), `total_amount`, `rejection_reason`, `version`, timestamp
- `order_items`: righe dell'ordine, con foreign key verso `orders`

Scelte di design:

- **`NUMERIC` per i prezzi**, mai `float`/`double`: i numeri in virgola mobile sbagliano i conti con i soldi.
- **`CHECK (stock >= 0)`**: il database rifiuta uno stock negativo anche in caso di bug nel codice.
- **Colonna `version`**: optimistic locking (JPA `@Version`), evita di vendere più pezzi di quelli disponibili con ordini concorrenti.
- **`order_items.product_id` senza foreign key**: `products` vive in un altro database di un altro servizio.
- **Snapshot di nome e prezzo in `order_items`**: se il prezzo cambia domani, l'ordine di ieri mantiene quello pagato.

### 5.5 Kafka

#### Concetti base

| Concetto | Cos'è |
|---|---|
| **Broker** | Il server Kafka, dove i messaggi sono salvati |
| **Topic** | Una categoria di messaggi con un nome (es. `orders.events`) |
| **Partizione** | Un topic è diviso in N liste ordinate, per lavorare in parallelo |
| **Offset** | Posizione progressiva di un messaggio nella sua partizione |
| **Producer / Consumer** | Chi scrive / chi legge |
| **Consumer group** | Consumer con lo stesso nome che si dividono le partizioni. Gruppi diversi ricevono **tutti** i messaggi |
| **Key** | Messaggi con la stessa key finiscono sempre nella stessa partizione → ordine garantito |

A differenza di una coda classica, **i messaggi letti non vengono cancellati**: restano nel topic (7 giorni di default) e ogni consumer group ricorda fin dove è arrivato.

#### Cosa è configurato (solo il broker)

| Impostazione | Valore | Significato |
|---|---|---|
| Nodi | 1 (`KAFKA_NODE_ID: 1`) | Un solo broker (in produzione ≥ 3) |
| Modalità | KRaft (`broker,controller`) | Senza Zookeeper: lo stesso processo gestisce messaggi e metadati |
| Listener | `localhost:9092` / `kafka:29092` | Uno per le app sul PC, uno per i container (vedi 5.6) |
| Replication factor | 1 | Una sola copia di ogni messaggio (con un broker non si può fare di più) |
| Auto-creazione topic | **disattivata** | Scrivere su un topic inesistente dà errore: i topic si creano esplicitamente |
| `KAFKA_LOG_DIRS` | `/var/lib/kafka/data` | Deve coincidere con il volume, altrimenti i dati finiscono in `/tmp` e si perdono a ogni `down` |

#### Cosa NON è ancora configurato

Topic del progetto, producer, consumer, formato dei messaggi: arriveranno nella **tappa 4**.
L'unico topic esistente è `test-tappa1`, creato a mano per verificare che Kafka funzioni.

Topic previsti:

| Topic | Scrive | Leggono | Eventi |
|---|---|---|---|
| `orders.events` | `order-service` | `inventory-service`, `notification-service` | `OrderCreated`, `OrderConfirmed`, `OrderRejected` |
| `inventory.events` | `inventory-service` | `order-service` | `StockReserved`, `StockRejected` |

### 5.6 Rete: localhost o nome del servizio?

Ogni container ha un IP in una rete virtuale Docker (`shop_default`) non raggiungibile direttamente dal PC.

- **Dal PC** (DBeaver, browser, IntelliJ) si passa dalle porte pubblicate con `ports: "PORTA_PC:PORTA_CONTAINER"` → si usa **`localhost`**.
- **Da un container** a un altro si usa il **nome del servizio** (`postgres`, `kafka`), risolto dal DNS interno di Docker.
- Dentro un container, `localhost` è **il container stesso**.

| Chi si collega | Postgres | Kafka |
|---|---|---|
| Dal PC | `localhost:5432` | `localhost:9092` (listener EXTERNAL) |
| Da un container | `postgres:5432` | `kafka:29092` (listener INTERNAL) |

Kafka ha bisogno di **due listener** perché, dopo la prima connessione, comunica al client l'indirizzo da usare (*advertised listener*): deve essere un indirizzo raggiungibile da chi lo riceve.

### 5.7 inventory-service

Catalogo prodotti e stock, porta **8081**. Struttura a strati, un package per funzionalità:

| Strato | Classe | Responsabilità |
|---|---|---|
| Web | `ProductController` | HTTP: URL, status code, header (`Location` sulla POST) |
| Business | `ProductService` | Regole (SKU unico) e transazioni (`@Transactional`) |
| Dati | `ProductRepository`, `Product` | Spring Data JPA / Hibernate ↔ tabella `products` |
| Contratto | `dto/` (record) | Cosa entra ed esce dalle API, con Bean Validation |
| Errori | `GlobalExceptionHandler` | Eccezioni → `ProblemDetail` (RFC 9457) |

**API**

| Metodo | URL | Successo | Errori |
|---|---|---|---|
| `GET` | `/products?page=0&size=20&sort=name` | 200, pagina (`content` + `page`) | 400 |
| `GET` | `/products/{id}` | 200 | 404, 400 (UUID non valido) |
| `POST` | `/products` | 201 + `Location` | 400 (validazione), 409 (SKU duplicato) |
| `PUT` | `/products/{id}` | 200 | 400, 404, 409 (modifica concorrente) |
| `DELETE` | `/products/{id}` | 204 | 404 |
| `GET` | `/actuator/health` | 200 `UP` | 503 se il DB non risponde |

**Meccanismi da ricordare**

- **Avvio**: Flyway verifica/applica le migrazioni, poi Hibernate (`ddl-auto: validate`) controlla che le entità coincidano con le tabelle. Se qualcosa non torna, il servizio non parte.
- **Configurazione**: `application.yml` legge `DB_HOST` (default `localhost`) e la password da `.env` o dalle variabili d'ambiente.
- **Dirty checking**: nella modifica non serve `save()`, Hibernate genera l'`UPDATE` al commit.
- **Optimistic locking**: `@Version` → `UPDATE ... WHERE version = ?`; una modifica concorrente produce 409 invece di sovrascrivere.
- **Doppia difesa**: vincoli nel DTO (messaggi chiari) e nel database (`CHECK`, `UNIQUE`).

**Test**: `ProductApiTests` avvia l'applicazione completa con un PostgreSQL vero in **Testcontainers** (`@ServiceConnection`) e chiama le API con `MockMvcTester`. Richiede Docker avviato:

```powershell
.\mvnw.cmd -pl inventory-service test
```

**Richieste di prova**: `inventory-service/products.http` (client HTTP di IntelliJ) oppure la collection `postman/event-driven-shop.postman_collection.json` (Postman → *Import*; con *Run collection* esegue tutte le richieste e i relativi test).

### 5.8 Il servizio in Docker

**`inventory-service/Dockerfile`**: build **multi-stage**.

1. Stadio `build` (`eclipse-temurin:25-jdk`): copia POM, Maven Wrapper e sorgenti, esegue `mvnw -pl inventory-service -am package`. Le dipendenze Maven restano in una cache (`--mount=type=cache`) tra una build e l'altra.
2. Stadio finale (`eclipse-temurin:25-jre`): contiene solo il jar, `curl` (per l'healthcheck) e un utente senza privilegi (`spring`).

Il build context è la **root del repository**, perché servono il POM parent e `mvnw`. `.dockerignore` esclude `.env` (mai segreti nelle immagini) e `target/`.

**`docker-compose.yml`**: servizio `inventory-service` nel profilo **`apps`**, quindi `docker compose up -d` avvia solo l'infrastruttura e il servizio si può lanciare da IntelliJ senza conflitti sulla porta 8081.

- `DB_HOST: postgres`: dal container il database si raggiunge con il nome del servizio (vedi 5.6).
- `depends_on` con `service_healthy`: parte solo quando Postgres è pronto.
- `healthcheck`: chiama `/actuator/health` → stato `healthy` / `unhealthy` in `docker ps`.
- **Debug remoto**: `JAVA_TOOL_OPTIONS` attiva l'agente JDWP sulla porta **5005**. In IntelliJ: *Run → Edit Configurations → + → Remote JVM Debug*, host `localhost`, porta `5005`, module classpath `inventory-service`. Solo per sviluppo; dopo una modifica al codice ricostruire l'immagine (`--build`), altrimenti i breakpoint non corrispondono.

---

## 6. Strumenti: DBeaver e Kafka UI

### DBeaver

Nuova connessione → PostgreSQL. Una connessione per database, con l'utente del servizio:

| Campo | orders | inventory |
|---|---|---|
| Host / Porta | `localhost` / `5432` | `localhost` / `5432` |
| Database | `orders_db` | `inventory_db` |
| Utente | `orders_user` | `inventory_user` |
| Password | valore di `ORDERS_DB_PASSWORD` nel `.env` | valore di `INVENTORY_DB_PASSWORD` nel `.env` |

Le tabelle sono in *Databases → \<db\> → Schemas → public → Tables*.
Il superutente `postgres` (password `POSTGRES_PASSWORD`) vede tutto: usarlo solo per amministrazione.

### Kafka UI

http://localhost:8085 → cluster **local**:

- **Brokers**: il nostro unico broker.
- **Topics**: elenco topic → *Messages* mostra partizione, offset, key e value di ogni messaggio; *Produce Message* permette di inviarne uno a mano.
- **Consumers**: i consumer group e il loro **lag** (messaggi ancora da leggere).

---

## 7. Comandi utili

```bash
docker compose up -d                              # avvia l'infrastruttura
docker compose ps                                 # stato dei container
docker compose logs -f kafka                      # log di un servizio
docker compose down                               # ferma tutto (i dati restano)
docker compose down -v                            # ferma e CANCELLA i dati (ripartenza da zero)

docker compose run --rm flyway-inventory          # applica le migrazioni di inventory_db
docker compose run --rm flyway-orders             # applica le migrazioni di orders_db
docker compose run --rm flyway-orders info        # stato delle migrazioni

docker compose --profile apps up -d --build       # (ri)costruisce e avvia i microservizi
docker compose logs -f inventory-service          # log del servizio
docker compose stop inventory-service             # ferma il container (per lanciarlo da IntelliJ)

docker exec shop-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

> Da **Git Bash**, i comandi `docker exec ... /opt/...` vanno preceduti da `MSYS_NO_PATHCONV=1`, altrimenti Git Bash trasforma il percorso in un percorso Windows.

---

## 8. Problemi noti e soluzioni

| Sintomo | Causa | Soluzione |
|---|---|---|
| `port is already allocated` | Porta già usata sul PC | Cambiare la porta nel `.env` (es. `POSTGRES_PORT=5433`) |
| I database non esistono | Script di init aggiunto/modificato dopo il primo avvio | `docker compose down -v` e `up -d` |
| Topic spariti dopo un riavvio | Kafka salvava in `/tmp` invece che nel volume | Risolto con `KAFKA_LOG_DIRS` |
| `release version 25 not supported` | `JAVA_HOME` punta a Java 8 | Impostare `JAVA_HOME` su JDK 25 nella sessione |
| Script `.sh` fallisce con `$'\r'` | Fine riga Windows (CRLF) | `.gitattributes` forza LF sui `.sh` |
| DBeaver: `database "..." does not exist` | Campo Database compilato male | Nel campo Database va `orders_db`, non il nome della variabile |
| DBeaver non mostra `inventory_db` | Isolamento voluto (`REVOKE CONNECT`) | Connessione separata con `inventory_user` |
| IntelliJ: `Cannot resolve method 'formatted'` | Progetto aperto con JDK 8 | Aprire la cartella del repo come progetto Maven e impostare l'SDK su JDK 25 |
| Postman: `ECONNREFUSED 127.0.0.1:8081` | Il servizio non è avviato (`docker compose up -d` avvia solo l'infrastruttura) | Avviarlo da IntelliJ o con `--profile apps` |
| Container `unhealthy` ma il servizio risponde | L'immagine JRE non contiene `curl` | `curl` installato nel Dockerfile |

---

## 9. Roadmap

- [x] **Tappa 1** — Progetto Maven multi-modulo, Docker Compose (Postgres, Kafka KRaft, Kafka UI), schema dei database con Flyway
- [x] **Tappa 2** — `inventory-service`: CRUD prodotti (Spring Web, JPA), test con Testcontainers, Dockerfile e debug remoto
- [ ] **Tappa 3** — `order-service`: creazione ordini in stato `PENDING`
- [ ] **Tappa 4** — Kafka e saga: `common-events`, topic, producer e consumer
- [ ] **Tappa 5** — `notification-service`
- [ ] **Tappa 6** — `api-gateway` (Spring Cloud Gateway, CORS)
- [ ] **Tappa 7** — Robustezza: idempotenza, retry e dead letter topic, outbox pattern, gestione errori
- [ ] **Tappa 8** — Aggiornamenti in tempo reale (SSE) e OpenAPI
- [ ] **Tappa 9** — Dockerfile dei servizi rimanenti, immagini più leggere e tracing distribuito
- [ ] Frontend React + Vite + TypeScript
- [ ] Server MCP (Spring AI)
