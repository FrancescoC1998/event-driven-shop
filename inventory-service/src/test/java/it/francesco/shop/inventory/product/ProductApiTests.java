package it.francesco.shop.inventory.product;

import it.francesco.shop.inventory.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test di integrazione: applicazione completa + PostgreSQL in Testcontainers + migrazioni Flyway.
 * MockMvcTester simula le chiamate HTTP senza avviare un server reale.
 *
 * I dati di esempio (V2__seed_products.sql) sono presenti: i test li usano in lettura
 * e creano prodotti con SKU univoci per non interferire tra loro.
 */
@SpringBootTest
//Crea un MockMvcTester, che simula le chiamate HTTP senza aprire una porta: la richiesta entra direttamente nel controller
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductApiTests {

    /** "Tastiera meccanica" dai dati di esempio. */
    private static final String SEED_KEYBOARD_ID = "11111111-1111-1111-1111-000000000001";

    @Autowired
    MockMvcTester mvc;

    @Test
    void listReturnsSeededProductsPaginated() {
        assertThat(mvc.get().uri("/products?size=5"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.content").asArray().hasSize(5);
                    json.assertThat().extractingPath("$.page.size").isEqualTo(5);
                    json.assertThat().extractingPath("$.page.totalElements").asNumber().satisfies(
                            total -> assertThat(total.intValue()).isGreaterThanOrEqualTo(8));
                });
    }

    @Test
    void getByIdReturnsProduct() {
        assertThat(mvc.get().uri("/products/{id}", SEED_KEYBOARD_ID))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.sku").isEqualTo("KB-001");
                    json.assertThat().extractingPath("$.price").asNumber().satisfies(
                            price -> assertThat(price.doubleValue()).isEqualTo(89.90));
                });
    }

    @Test
    void getUnknownIdReturns404ProblemDetail() {
        assertThat(mvc.get().uri("/products/{id}", UUID.randomUUID()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.status").isEqualTo(404);
    }

    @Test
    void getWithInvalidUuidReturns400() {
        assertThat(mvc.get().uri("/products/non-un-uuid"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createReturns201WithLocationAndProductIsReadable() {
        MvcTestResult result = mvc.post().uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku": "TEST-CREATE", "name": "Prodotto di test", "price": 10.50, "stock": 3}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        String location = result.getResponse().getHeader("Location");
        assertThat(location).contains("/products/");

        assertThat(mvc.get().uri(location))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").isEqualTo("Prodotto di test");
    }

    @Test
    void createWithDuplicateSkuReturns409() {
        assertThat(mvc.post().uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku": "KB-001", "name": "Doppione", "price": 1.00, "stock": 1}
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.detail").asString().contains("KB-001");
    }

    @Test
    void createWithInvalidDataReturns400WithFieldErrors() {
        assertThat(mvc.post().uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku": "", "name": "Prezzo negativo", "price": -5, "stock": 1}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.errors.sku").isNotNull();
                    json.assertThat().extractingPath("$.errors.price").isNotNull();
                });
    }

    @Test
    void updateChangesProduct() {
        String id = createProduct("TEST-UPDATE");

        assertThat(mvc.put().uri("/products/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Nome aggiornato", "price": 20.00, "stock": 7}
                        """))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.name").isEqualTo("Nome aggiornato");
                    json.assertThat().extractingPath("$.stock").isEqualTo(7);
                    json.assertThat().extractingPath("$.sku").isEqualTo("TEST-UPDATE");
                });
    }

    @Test
    void deleteRemovesProduct() {
        String id = createProduct("TEST-DELETE");

        assertThat(mvc.delete().uri("/products/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/products/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void healthIsUp() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    private String createProduct(String sku) {
        MvcTestResult result = mvc.post().uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku": "%s", "name": "Da modificare", "price": 1.00, "stock": 1}
                        """.formatted(sku))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        String location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
