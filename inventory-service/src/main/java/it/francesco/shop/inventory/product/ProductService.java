package it.francesco.shop.inventory.product;

import it.francesco.shop.inventory.product.dto.CreateProductRequest;
import it.francesco.shop.inventory.product.dto.ProductResponse;
import it.francesco.shop.inventory.product.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Logica di business sui prodotti.
 * Ogni metodo pubblico è una transazione: o va tutto a buon fine, o il DB torna com'era.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    // Dependency injection via costruttore: Spring passa il repository, nessun @Autowired necessario
    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Page<ProductResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(ProductResponse::from);
    }

    public ProductResponse findById(UUID id) {
        return ProductResponse.from(getOrThrow(id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        Product product = new Product(
                request.sku(), request.name(), request.description(), request.price(), request.stock());
        // saveAndFlush: esegue subito l'INSERT, così id e timestamp sono valorizzati nella risposta
        return ProductResponse.from(repository.saveAndFlush(product));
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = getOrThrow(id);
        // Nessun save() esplicito: l'entità è "managed", Hibernate rileva le modifiche
        // e fa l'UPDATE al commit (dirty checking). Il flush serve solo ad aggiornare updatedAt/version subito.
        product.update(request.name(), request.description(), request.price(), request.stock());
        repository.flush();
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(getOrThrow(id));
    }

    private Product getOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
