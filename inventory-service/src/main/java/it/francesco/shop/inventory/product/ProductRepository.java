package it.francesco.shop.inventory.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Nessuna implementazione da scrivere: Spring Data la genera a runtime.
 * {@code JpaRepository} fornisce findAll (anche paginato), findById, save, delete, ...
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /** Query dedotta dal nome del metodo: SELECT count(*) > 0 FROM products WHERE sku = ? */
    boolean existsBySku(String sku);
}
