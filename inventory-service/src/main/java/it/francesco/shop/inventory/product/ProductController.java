package it.francesco.shop.inventory.product;

import it.francesco.shop.inventory.product.dto.CreateProductRequest;
import it.francesco.shop.inventory.product.dto.ProductResponse;
import it.francesco.shop.inventory.product.dto.UpdateProductRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /** GET /products?page=0&size=20&sort=price,desc */
    @GetMapping
    public PagedModel<ProductResponse> findAll(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        // PagedModel: formato JSON stabile {content: [...], page: {size, number, totalElements, totalPages}}
        return new PagedModel<>(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    /** 201 Created + header Location con l'URL del nuovo prodotto. */
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        return service.update(id, request);
    }

    /** 204 No Content: eliminato, niente da restituire. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
