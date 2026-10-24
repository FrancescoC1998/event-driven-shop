#!/bin/bash
# Eseguito da Postgres SOLO al primo avvio (quando il volume dei dati è vuoto).
# Crea un utente e un database per ogni microservizio: "un database per servizio".
# Le password arrivano dalle variabili d'ambiente definite in docker-compose.yml (.env).
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    -- order-service
    CREATE USER orders_user WITH PASSWORD '${ORDERS_DB_PASSWORD}';
    CREATE DATABASE orders_db OWNER orders_user;
    -- Di default chiunque può connettersi a qualsiasi database: lo impediamo
    REVOKE CONNECT ON DATABASE orders_db FROM PUBLIC;

    -- inventory-service
    CREATE USER inventory_user WITH PASSWORD '${INVENTORY_DB_PASSWORD}';
    CREATE DATABASE inventory_db OWNER inventory_user;
    REVOKE CONNECT ON DATABASE inventory_db FROM PUBLIC;
EOSQL

echo "Database orders_db e inventory_db creati."
