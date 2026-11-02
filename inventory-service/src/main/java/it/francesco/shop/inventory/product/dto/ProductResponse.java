package it.francesco.shop.inventory.product.dto;

import it.francesco.shop.inventory.product.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Il "contratto pubblico" dell'API: cosa vede il client.
 * L'entità resta un dettaglio interno (es. il campo version non viene esposto).
 */
public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stock,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
