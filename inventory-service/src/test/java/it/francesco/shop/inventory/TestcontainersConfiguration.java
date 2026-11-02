package it.francesco.shop.inventory;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Avvia un PostgreSQL vero in un container usa e getta per i test (stessa versione del docker-compose).
 * {@code @ServiceConnection} fa sì che Spring Boot usi URL, utente e password di questo container
 * al posto di quelli in application.yml. Flyway applica le migrazioni al container all'avvio del contesto.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
    }
}
