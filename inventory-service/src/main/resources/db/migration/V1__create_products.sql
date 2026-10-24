-- Catalogo prodotti e disponibilità di magazzino (inventory-service).
-- Regola Flyway: un file già eseguito NON si modifica più; per cambiare lo schema si aggiunge V3, V4...

CREATE TABLE products (
    id          UUID           PRIMARY KEY,
    sku         VARCHAR(50)    NOT NULL UNIQUE,                 -- codice prodotto leggibile (es. KB-001)
    name        VARCHAR(200)   NOT NULL,
    description TEXT,
    price       NUMERIC(10, 2) NOT NULL CHECK (price >= 0),     -- NUMERIC, mai float/double per i soldi
    stock       INTEGER        NOT NULL CHECK (stock >= 0),     -- il DB impedisce stock negativi anche in caso di bug
    version     BIGINT         NOT NULL DEFAULT 0,              -- optimistic locking (JPA @Version)
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);
