package it.francesco.shop.inventory.product;

import java.util.UUID;

/** Il prodotto richiesto non esiste. Tradotta in HTTP 404 da GlobalExceptionHandler. */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID id) {
        super("Prodotto " + id + " non trovato");
    }
}
