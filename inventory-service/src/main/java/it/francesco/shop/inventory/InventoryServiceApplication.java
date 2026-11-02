package it.francesco.shop.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto di avvio del servizio. {@code @SpringBootApplication} attiva la configurazione automatica
 * e la scansione dei componenti in questo package e nei sotto-package.
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
