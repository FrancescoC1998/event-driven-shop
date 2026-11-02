package it.francesco.shop.inventory.product;

/** Esiste già un prodotto con lo stesso SKU. Tradotta in HTTP 409 da GlobalExceptionHandler. */
public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("Esiste già un prodotto con SKU " + sku);
    }
}
