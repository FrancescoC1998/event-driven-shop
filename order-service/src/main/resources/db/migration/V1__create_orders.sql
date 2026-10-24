-- Ordini e righe d'ordine (order-service).

CREATE TABLE orders (
    id               UUID           PRIMARY KEY,
    customer_name    VARCHAR(200)   NOT NULL,
    customer_email   VARCHAR(320)   NOT NULL,
    -- Ciclo di vita gestito dalla saga: PENDING -> CONFIRMED oppure REJECTED
    status           VARCHAR(20)    NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED')),
    total_amount     NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    rejection_reason TEXT,                                   -- valorizzato solo se REJECTED (es. stock insufficiente)
    version          BIGINT         NOT NULL DEFAULT 0,      -- optimistic locking (JPA @Version)
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at DESC);

CREATE TABLE order_items (
    id           UUID           PRIMARY KEY,
    -- FK interna allo stesso database: consentita (ordini e righe sono dello stesso servizio)
    order_id     UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    -- NESSUNA foreign key verso products: quella tabella vive in inventory_db, di un altro servizio.
    product_id   UUID           NOT NULL,
    -- Copia ("snapshot") dei dati del prodotto al momento dell'ordine:
    -- se domani il prezzo cambia, l'ordine di ieri deve mantenere quello pagato.
    product_sku  VARCHAR(50)    NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    quantity     INTEGER        NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
